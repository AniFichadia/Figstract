package com.anifichadia.figmaimporter

import com.anifichadia.figmaimporter.apiclient.ApiResponse
import com.anifichadia.figmaimporter.figma.api.FigmaApi
import com.anifichadia.figmaimporter.figma.model.Error
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse
import com.anifichadia.figmaimporter.figma.model.GetImageResponse
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.Instruction
import com.anifichadia.figmaimporter.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.invoke
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.model.tracking.ProcessingRecordRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.util.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import kotlin.math.ceil
import kotlin.math.min

class FigmaImporter(
    private val figmaApi: FigmaApi,
    private val downloaderHttpClient: HttpClient,
    private val processingRecordRepository: ProcessingRecordRepository,
) {
    suspend fun importFromFigma(handlers: List<FigmaFileHandler>) {
        val importFlow = handlers
            .map { handler -> createImportFlowForHandler(handler) }
            .merge()

        coroutineScope {
            importFlow.launchIn(this)
        }
    }

    suspend fun importFromFigma(handler: FigmaFileHandler) {
        val importFlow = createImportFlowForHandler(handler)

        coroutineScope {
            importFlow.launchIn(this)
        }
    }

    private fun createImportFlowForHandler(handler: FigmaFileHandler): Flow<Unit> {
        val figmaFile = handler.figmaFile
        var lastUpdated: OffsetDateTime? = null

        val fileFlow = createFigmaFileFlow(handler)
            .onEach { (_, response) ->
                response.onSuccess {
                    lastUpdated = this.body.lastModified
                }
            }
        val exportFlow = createExportFlow(fileFlow)
        val importFlow = createImportFlow(exportFlow)
            .onCompletion {
                lastUpdated?.let {
                    processingRecordRepository.updateRecord(figmaFile, it)
                }
            }

        return importFlow
    }

    private fun createFigmaFileFlow(handler: FigmaFileHandler): Flow<Pair<FigmaFileHandler, ApiResponse<GetFilesResponse>>> {
        return flowOf(handler)
            .flowOn(Dispatchers.Default)
            .map { handler ->
                logger.debug { "Fetching ${handler.figmaFile}: Start" }
                val getFileApiResponse = figmaApi.getFile(
                    key = handler.figmaFile,
                )
                logger.info { "Fetching ${handler.figmaFile}: Finish ${getFileApiResponse.isSuccess()}" }
                getFileApiResponse.logError { "Fetching ${handler.figmaFile}" }

                handler to getFileApiResponse
            }.flowOn(Dispatchers.IO)
            // TODO: error handling
            .filter { (_, apiResponse) -> apiResponse.isSuccess() }
            .flowOn(Dispatchers.Default)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createExportFlow(fileFlow: Flow<Pair<FigmaFileHandler, ApiResponse<GetFilesResponse>>>): Flow<ExportOutput> {
        val fileImportChunkCreatingFlow = fileFlow
            .filter { (handler, getFileApiResponse) ->
                val response = getFileApiResponse.successBodyOrThrow()
                val record = processingRecordRepository.readRecord(handler.figmaFile)

                val processFile = if (record != null) {
                    response.lastModified > record.lastProcessed
                } else {
                    true
                }
                logger.info { "Should process ${handler.figmaFile}: $processFile" }

                processFile
            }
            .map { (handler, getFileApiResponse) ->
                val response = getFileApiResponse.successBodyOrThrow()
                val exportInstructions = handler.extractor.extract(response)

                handler to exportInstructions
            }
            .map { (handler, instructions) ->
                val exportsByConfig = instructions.groupBy { it.export.config }

                handler to exportsByConfig
            }
            .flatMapConcat { (handler, exportsByConfig) ->
                val assetsPerChunk = handler.assetsPerChunk

                exportsByConfig.entries
                    .map { (exportConfig, exportInstructions) ->
                        val exportDataSize = exportInstructions.size
                        val chunkCount = ceil(exportDataSize.toDouble() / assetsPerChunk).toInt()
                        val chunks = (0..<chunkCount)
                            .map { chunkIndex ->
                                val start = chunkIndex * assetsPerChunk
                                val end = min(start + assetsPerChunk, exportDataSize)

                                start..<end
                            }
                            .filter { chunkRange -> !chunkRange.isEmpty() }
                            .map { chunkRange -> exportConfig to exportInstructions.slice(chunkRange) }

                        chunks
                    }
                    .flatten()
                    .mapIndexed { index, (exportConfig, chunk) ->
                        Chunk(
                            handler = handler,
                            exportConfig = exportConfig,
                            chunkIndex = index,
                            instructions = chunk,
                        )
                    }
                    .asFlow()
            }
            .flowOn(Dispatchers.Default)

        // TODO: this blocks the flow from completing when used, preventing the process from finishing.
        //  It'd be good to have a retry mechanism that reuses the chunkFlow
//        val retriedChunkFlow = MutableSharedFlow<Chunk>()

        val chunkFlow = merge(
            fileImportChunkCreatingFlow,
//            retriedChunkFlow.takeWhile { false },
        )

        val exportFlow = chunkFlow
            .flatMapMerge { chunk ->
                val handler = chunk.handler
                val exportConfig = chunk.exportConfig
                val chunkIndex = chunk.chunkIndex
                val instructions = chunk.instructions
                val isRetry = chunk.isRetry

                flow {
                    logger.debug { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: ${isRetry}, $exportConfig: Started" }

                    val getImagesApiResponse = figmaApi.getImages(
                        key = handler.figmaFile,
                        ids = instructions.map { it.export.nodeId },
                        format = exportConfig.format,
                        scale = exportConfig.scale,
                    )
                    getImagesApiResponse.logError { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: ${isRetry}, $exportConfig" }

                    if (!(getImagesApiResponse.errorMatches(GetImageResponse.KnownErrors.tooManyImages)) || instructions.size <= 1) {
                        emit(
                            GetImagesResult(
                                originalChunk = chunk,
                                expectedInstructions = instructions,
                                getImageResponse = getImagesApiResponse,
                            )
                        )

                        logger.info { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: ${isRetry}, $exportConfig: Finish ${getImagesApiResponse.isSuccess()}" }
                        getImagesApiResponse.logError { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, chunkSize: ${instructions.size}, $exportConfig" }
                    } else {
                        logger.debug { "Getting images failed, falling back to retrieving images individually" }
                        instructions.forEach { instruction ->
//                            retriedChunkFlow.emit(
//                                chunk.copy(
//                                    instructions = listOf(instruction),
//                                    isRetry = true,
//                                )
//                            )

                            val instructions = listOf(instruction)
                            emit(
                                GetImagesResult(
                                    originalChunk = chunk.copy(
                                        instructions = instructions,
                                        isRetry = true,
                                    ),
                                    expectedInstructions = instructions,
                                    getImageResponse = withContext(Dispatchers.IO) {
                                        figmaApi.getImages(
                                            key = handler.figmaFile,
                                            ids = instructions.map { it.export.nodeId },
                                            format = exportConfig.format,
                                            scale = exportConfig.scale,
                                        )
                                    },
                                )
                            )
                        }
                    }
                }
                    // TODO: error handling
                    .filter { result -> result.getImageResponse.isSuccess() }
                    .flowOn(Dispatchers.IO)
                    .map { result ->
                        val body = result.getImageResponse.successBodyOrThrow()
                        val instructions = result.expectedInstructions

                        if (body.images.keys != instructions.map { it.export.nodeId }.toSet()) {
                            error("Not all images retrieved")
                        }

                        body.images.entries.asFlow()
                            .mapNotNull { (nodeId, imageUrl) ->
                                if (imageUrl == null) {
                                    // TODO: error handling
                                    logger.error { "Fetching image for $nodeId failed due to null image URL" }

                                    return@mapNotNull null
                                }

                                val instructionsForNode =
                                    instructions.filter { it.export.nodeId == nodeId }

                                try {
                                    val imageResponse = downloaderHttpClient.get(
                                        urlString = imageUrl,
                                    )
                                    val bodyBytes = imageResponse.bodyAsChannel().toByteArray()

                                    instructionsForNode.map { instruction -> instruction to bodyBytes }
                                } catch (e: Throwable) {
                                    logger.error(e) { "Failed fetching image for $nodeId at $imageUrl" }
                                    // TODO: error handling
                                    null
                                }
                            }
                            .flatMapConcat { it.asFlow() }
                            .flowOn(Dispatchers.IO)
                    }
                    .flattenConcat()
                    .map { (instruction, data) ->
                        ExportOutput(
                            handler = handler,
                            instruction = instruction,
                            data = data,
                        )
                    }
                    .flowOn(Dispatchers.Default)
            }

        return exportFlow
    }

    private fun createImportFlow(exportFlow: Flow<ExportOutput>): Flow<Unit> {
        return exportFlow
            .map { (handler, instruction, data) ->
                logger.debug { "Importing ${handler.figmaFile} $instruction: Started" }
                try {
                    instruction.import
                        .pipelineSteps
                        .then()
                        .invoke(instruction, data)
                    logger.info { "Importing ${handler.figmaFile} $instruction: Finished true" }
                } catch (e: Throwable) {
                    logger.error(e) { "Importing ${handler.figmaFile} $instruction: Finished false" }
                    // TODO: error handling
                }
            }
            .flowOn(Dispatchers.IO)
    }

    private data class Chunk(
        val handler: FigmaFileHandler,
        val exportConfig: ExportConfig,
        val chunkIndex: Int,
        val instructions: List<Instruction>,
        val isRetry: Boolean = false,
    )

    private data class GetImagesResult(
        val originalChunk: Chunk,
        val expectedInstructions: List<Instruction>,
        val getImageResponse: ApiResponse<GetImageResponse>,
    )

    private data class ExportOutput(
        val handler: FigmaFileHandler,
        val instruction: Instruction,
        val data: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ExportOutput

            if (handler != other.handler) return false
            if (instruction != other.instruction) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = handler.hashCode()
            result = 31 * result + instruction.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    companion object {
        private suspend fun <R> ApiResponse<R>.errorMatches(error: Error): Boolean {
            return this is ApiResponse.Failure.ResponseError && this.errorBodyAs<Error>() == error
        }

        private fun <R> ApiResponse<R>.logError(message: () -> String) {
            if (this is ApiResponse.Failure) {
                logger.error(this.asException(), message)
            }
        }
    }
}

private val logger = KotlinLogging.logger("FigmaImporter")
