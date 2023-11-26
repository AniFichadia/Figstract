package com.anifichadia.figmaimporter

import com.anifichadia.figmaimporter.apiclient.ApiResponse
import com.anifichadia.figmaimporter.figma.api.FigmaApi
import com.anifichadia.figmaimporter.figma.api.FigmaApiProxyWithFlowControl
import com.anifichadia.figmaimporter.figma.api.FigmaApiProxyWithFlowControl.Companion.DEFAULT_CONCURRENCY_LIMIT
import com.anifichadia.figmaimporter.figma.api.KnownErrors.errorMatches
import com.anifichadia.figmaimporter.figma.model.GetFilesResponse
import com.anifichadia.figmaimporter.figma.model.GetImageResponse
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.Instruction
import com.anifichadia.figmaimporter.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.model.tracking.ProcessingRecordRepository
import com.anifichadia.figmaimporter.util.createLogger
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
import kotlinx.coroutines.flow.onStart
import java.time.OffsetDateTime
import kotlin.coroutines.CoroutineContext
import kotlin.math.ceil
import kotlin.math.min

class FigmaImporter(
    figmaApi: FigmaApi,
    private val downloaderHttpClient: HttpClient,
    private val processingRecordRepository: ProcessingRecordRepository,
    figmaApiConcurrencyLimit: Int = DEFAULT_CONCURRENCY_LIMIT,
    private val defaultContext: CoroutineContext = Dispatchers.Default,
    private val networkContext: CoroutineContext = Dispatchers.IO,
    private val importPipelineContext: CoroutineContext = Dispatchers.IO,
) {
    private val figmaApi = FigmaApiProxyWithFlowControl(figmaApi, figmaApiConcurrencyLimit)

    suspend fun importFromFigma(handlers: List<FigmaFileHandler>) {
        val importFlow = handlers
            .map { handler -> createProcessingFlowForHandler(handler) }
            .merge()

        coroutineScope {
            importFlow.launchIn(this)
        }
    }

    private fun createProcessingFlowForHandler(handler: FigmaFileHandler): Flow<Unit> {
        val figmaFile = handler.figmaFile
        var lastUpdated: OffsetDateTime? = null

        val handlersFlow = flowOf(handler)

        val fileFlow = createFigmaFileFlow(handlersFlow)
            .onStart {
                handler.lifecycle.onStarted()
                handler.lifecycle.onFileRetrievalStarted()
            }
            .onEach { (_, response) ->
                response.onSuccess {
                    lastUpdated = this.body.lastModified
                }
            }
            .onCompletion { handler.lifecycle.onFileRetrievalFinished() }

        val exportFlow = createExportFlow(fileFlow)
            .onStart { handler.lifecycle.onExportStarted() }
            .onCompletion { handler.lifecycle.onExportFinished() }

        val importFlow = createImportFlow(exportFlow)
            .onStart { handler.lifecycle.onImportStarted() }
            .onCompletion {
                handler.lifecycle.onImportFinished()

                lastUpdated?.let {
                    processingRecordRepository.updateRecord(figmaFile, it)
                }

                handler.lifecycle.onFinished()
            }

        return importFlow
    }

    private fun createFigmaFileFlow(handlers: Flow<FigmaFileHandler>): Flow<Pair<FigmaFileHandler, ApiResponse<GetFilesResponse>>> {
        return handlers
            .flowOn(defaultContext)
            .map { handler ->
                logger.debug { "Fetching ${handler.figmaFile}: Start" }
                val getFileApiResponse = figmaApi.getFile(
                    key = handler.figmaFile,
                )
                logger.info { "Fetching ${handler.figmaFile}: Finish ${getFileApiResponse.isSuccess()}" }
                getFileApiResponse.logError { "Fetching ${handler.figmaFile}" }

                handler to getFileApiResponse
            }
            .flowOn(networkContext)
            // TODO: error handling
            .filter { (_, apiResponse) -> apiResponse.isSuccess() }
            .flowOn(defaultContext)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createExportFlow(fileFlow: Flow<Pair<FigmaFileHandler, ApiResponse<GetFilesResponse>>>): Flow<ExportOutput> {
        val instructionsFlow = fileFlow
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
                getFileApiResponse as ApiResponse.Success
                val response = getFileApiResponse.body
                val bodyText = getFileApiResponse.bodyText
                val exportInstructions = handler.extractor.extract(response, bodyText)

                handler to exportInstructions
            }

        val fileImportChunkCreatingFlow = instructionsFlow
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
            .flowOn(defaultContext)

        val exportFlow = fileImportChunkCreatingFlow
            .flatMapMerge { chunk ->
                val handler = chunk.handler
                val exportConfig = chunk.exportConfig
                val chunkIndex = chunk.chunkIndex
                val instructions = chunk.instructions
                val isRetry = chunk.isRetry

                val firstAttemptFlow = flow {
                    logger.debug { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: ${isRetry}, $exportConfig: Started" }

                    val getImagesApiResponse = figmaApi.getImages(
                        key = handler.figmaFile,
                        ids = instructions.map { it.export.nodeId },
                        format = exportConfig.format,
                        scale = exportConfig.scale,
                        contentsOnly = exportConfig.contentsOnly,
                    )
                    getImagesApiResponse.logError { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: ${isRetry}, $exportConfig" }

                    emit(
                        GetImagesResult(
                            originalChunk = chunk,
                            expectedInstructions = instructions,
                            getImageResponse = getImagesApiResponse,
                        )
                    )

                    logger.info { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: ${isRetry}, $exportConfig: Finish ${getImagesApiResponse.isSuccess()}" }
                }
                    .flowOn(networkContext)

                val retryFlow = firstAttemptFlow
                    .filter { it.getImageResponse.errorMatches(GetImageResponse.KnownErrors.tooManyImages) && it.expectedInstructions.size > 1 }
                    .flatMapMerge { it.expectedInstructions.asFlow() }
                    .map { instruction ->
                        val instructions = listOf(instruction)

                        logger.debug { "Getting images as retry ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: true, $exportConfig: Started" }
                        val getImagesApiResponse = figmaApi.getImages(
                            key = handler.figmaFile,
                            ids = instructions.map { it.export.nodeId },
                            format = exportConfig.format,
                            scale = exportConfig.scale,
                            contentsOnly = exportConfig.contentsOnly,
                        )
                        getImagesApiResponse.logError { "Getting images as retry ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: true, $exportConfig" }
                        logger.info { "Getting images as retry ${handler.figmaFile}, chunkIndex: $chunkIndex, isRetry: true, $exportConfig: Finish ${getImagesApiResponse.isSuccess()}" }

                        GetImagesResult(
                            originalChunk = chunk.copy(
                                instructions = instructions,
                                isRetry = true,
                            ),
                            expectedInstructions = instructions,
                            getImageResponse = getImagesApiResponse,
                        )
                    }
                    .flowOn(networkContext)

                merge(
                    firstAttemptFlow.filter { it.getImageResponse.isSuccess() },
                    retryFlow,
                )
                    // TODO: error handling
                    .filter { result -> result.getImageResponse.isSuccess() }
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
                            .flowOn(networkContext)
                    }
                    .flattenConcat()
                    .map { (instruction, data) ->
                        ExportOutput(
                            handler = handler,
                            instruction = instruction,
                            data = data,
                        )
                    }
                    .flowOn(defaultContext)
            }

        return exportFlow
    }

    private fun createImportFlow(exportFlow: Flow<ExportOutput>): Flow<Unit> {
        return exportFlow
            .map { (handler, instruction, data) ->
                logger.debug { "Importing ${handler.figmaFile} $instruction: Started" }
                try {
                    instruction.import.pipeline.execute(instruction, data)
                    logger.info { "Importing ${handler.figmaFile} $instruction: Finished true" }
                } catch (e: Throwable) {
                    logger.error(e) { "Importing ${handler.figmaFile} $instruction: Finished false" }
                    // TODO: error handling
                }
            }
            .flowOn(importPipelineContext)
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
        private val logger = createLogger("FigmaImporter")

        private fun <R> ApiResponse<R>.logError(message: () -> String) {
            if (this is ApiResponse.Failure) {
                logger.error(this.asException(), message)
            }
        }
    }
}
