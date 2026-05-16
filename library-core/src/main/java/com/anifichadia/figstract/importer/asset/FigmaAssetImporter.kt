package com.anifichadia.figstract.importer.asset

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.api.FigmaApi
import com.anifichadia.figstract.figma.model.GetFilesResponse
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.ImportResult
import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figstract.importer.asset.reporting.FigmaImportReport
import com.anifichadia.figstract.importer.asset.reporting.ImportReportRepository
import com.anifichadia.figstract.importer.getFileWithBranchName
import com.anifichadia.figstract.model.tracking.ProcessingRecordRepository
import com.anifichadia.figstract.util.createLogger
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
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

class FigmaAssetImporter(
    private val figmaApi: FigmaApi,
    private val downloaderHttpClient: HttpClient,
    private val processingRecordRepository: ProcessingRecordRepository,
    private val importReportRepository: ImportReportRepository,
    private val defaultContext: CoroutineContext = Dispatchers.Default,
    private val networkContext: CoroutineContext = Dispatchers.IO,
    private val importPipelineContext: CoroutineContext = Dispatchers.IO,
) {
    suspend fun importFromFigma(handlers: List<AssetFileHandler>) {
        val reports = handlers.associate { handler ->
            handler.figmaFile to FigmaImportReport(handler.figmaFile)
        }

        val importFlow = handlers
            .map { handler -> createProcessingFlowForHandler(handler, reports.getValue(handler.figmaFile)) }
            .merge()

        coroutineScope {
            importFlow.launchIn(this)
        }

        for (report in reports.values) {
            importReportRepository.save(report)
            logger.info { report.summary() }

            if (report.hasFailures()) {
                logger.error { "Import failures for ${report.figmaFile}: ${report.failures().size} failure(s)" }
            }
        }
    }

    private fun createProcessingFlowForHandler(
        handler: AssetFileHandler,
        report: FigmaImportReport,
    ): Flow<Unit> {
        return flow {
            val resolvedHandler = assetFileHandlerForBranch(handler)
            val flow = createResolvedProcessingFlow(resolvedHandler, report)
            emitAll(flow)
        }
    }

    private suspend fun assetFileHandlerForBranch(handler: AssetFileHandler): AssetFileHandler {
        val figmaFileBranchName = handler.figmaFileBranchName ?: return handler

        val branchKey = figmaApi.getFileWithBranchName(
            key = handler.figmaFile,
            branchName = figmaFileBranchName,
            logger = logger,
        )

        return handler.withResolvedBranchKey(branchKey)
    }

    private fun createResolvedProcessingFlow(
        handler: AssetFileHandler,
        report: FigmaImportReport,
    ): Flow<Unit> {
        val figmaFile = handler.figmaFile
        var lastUpdated: OffsetDateTime? = null

        val handlersFlow = flowOf(handler)

        val fileFlow = createFigmaFileFlow(handlersFlow, report)
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

        val exportFlow = createExportFlow(fileFlow, report)
            .onStart { handler.lifecycle.onExportStarted() }
            .onCompletion { handler.lifecycle.onExportFinished() }

        val importFlow = createImportFlow(exportFlow, report)
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

    private fun createFigmaFileFlow(
        handlers: Flow<AssetFileHandler>,
        report: FigmaImportReport,
    ): Flow<Pair<AssetFileHandler, ApiResponse<GetFilesResponse>>> {
        return handlers
            .flowOn(defaultContext)
            .map { handler ->
                logger.debug { "Fetching ${handler.figmaFile}: Start" }
                val getFileApiResponse = figmaApi.getFile(
                    key = handler.figmaFile,
                    version = handler.figmaFileVersion,
                )
                logger.info { "Fetching ${handler.figmaFile}: Finish ${getFileApiResponse.isSuccess()}" }
                getFileApiResponse.logError { "Fetching ${handler.figmaFile}" }

                if (!getFileApiResponse.isSuccess()) {
                    report.record(
                        ImportResult.Failure.GetFileFailed(
                            figmaFile = handler.figmaFile,
                            cause = (getFileApiResponse as ApiResponse.Failure).asException(),
                        )
                    )
                }

                handler to getFileApiResponse
            }
            .flowOn(networkContext)
            .filter { (_, apiResponse) -> apiResponse.isSuccess() }
            .flowOn(defaultContext)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createExportFlow(
        fileFlow: Flow<Pair<AssetFileHandler, ApiResponse<GetFilesResponse>>>,
        report: FigmaImportReport,
    ): Flow<ExportOutput> {
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

                exportsByConfig
                    .entries
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

                flow {
                    logger.debug { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, $exportConfig: Started" }

                    val getImagesApiResponse = figmaApi.getImages(
                        key = handler.figmaFile,
                        ids = instructions.map { it.export.nodeId },
                        format = exportConfig.format,
                        scale = exportConfig.scale,
                        contentsOnly = exportConfig.contentsOnly,
                    )
                    getImagesApiResponse.logError { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, $exportConfig" }
                    logger.info { "Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, $exportConfig: Finish ${getImagesApiResponse.isSuccess()}" }

                    if (!getImagesApiResponse.isSuccess()) {
                        val cause = (getImagesApiResponse as ApiResponse.Failure).asException()
                        instructions.forEach { instruction ->
                            report.record(
                                ImportResult.Failure.NodeFailure.GetImagesFailed(
                                    figmaFile = handler.figmaFile,
                                    nodeId = instruction.export.nodeId,
                                    cause = cause,
                                )
                            )
                        }
                        return@flow
                    }

                    val body = getImagesApiResponse.successBodyOrThrow()
                    body.images.entries.asFlow()
                        .mapNotNull { (nodeId, imageUrl) ->
                            val instructionsForNode = instructions.filter { it.export.nodeId == nodeId }

                            if (imageUrl == null) {
                                logger.error { "Fetching image for $nodeId failed due to null image URL" }
                                instructionsForNode.forEach { _ ->
                                    report.record(
                                        ImportResult.Failure.NodeFailure.NoImageUrl(
                                            figmaFile = handler.figmaFile,
                                            nodeId = nodeId,
                                        )
                                    )
                                }
                                return@mapNotNull null
                            }

                            try {
                                val imageResponse = downloaderHttpClient.get(
                                    urlString = imageUrl,
                                )
                                val bodyBytes = imageResponse.bodyAsChannel().toByteArray()

                                instructionsForNode.map { instruction ->
                                    ExportOutput(
                                        handler = handler,
                                        instruction = instruction,
                                        data = bodyBytes,
                                        imageUrl = imageUrl,
                                    )
                                }
                            } catch (e: Throwable) {
                                logger.error(e) { "Failed fetching image for $nodeId at $imageUrl" }
                                instructionsForNode.forEach { _ ->
                                    report.record(
                                        ImportResult.Failure.NodeFailure.DownloadFailed(
                                            figmaFile = handler.figmaFile,
                                            nodeId = nodeId,
                                            imageUrl = imageUrl,
                                            cause = e,
                                        )
                                    )
                                }
                                null
                            }
                        }
                        .flatMapConcat { it.asFlow() }
                        .collect { emit(it) }
                }
                    .flowOn(networkContext)
            }

        return exportFlow
    }

    private fun createImportFlow(
        exportFlow: Flow<ExportOutput>,
        report: FigmaImportReport,
    ): Flow<Unit> {
        return exportFlow
            .map { exportOutput ->
                val (handler, instruction, data, imageUrl) = exportOutput
                logger.debug { "Importing ${handler.figmaFile} $instruction: Started" }
                try {
                    instruction.import.pipeline.execute(instruction, data)
                    report.record(
                        ImportResult.Success(
                            figmaFile = handler.figmaFile,
                            nodeId = instruction.export.nodeId,
                            imageUrl = imageUrl,
                            instruction = instruction,
                        )
                    )
                    logger.info { "Importing ${handler.figmaFile} $instruction: Finished true" }
                } catch (e: Throwable) {
                    logger.error(e) { "Importing ${handler.figmaFile} $instruction: Finished false" }
                    report.record(
                        ImportResult.Failure.NodeFailure.ImportPipelineFailed(
                            figmaFile = handler.figmaFile,
                            nodeId = instruction.export.nodeId,
                            instruction = instruction,
                            cause = e,
                        )
                    )
                }
            }
            .flowOn(importPipelineContext)
    }

    private data class Chunk(
        val handler: AssetFileHandler,
        val exportConfig: ExportConfig,
        val chunkIndex: Int,
        val instructions: List<Instruction>,
    )

    private data class ExportOutput(
        val handler: AssetFileHandler,
        val instruction: Instruction,
        val data: ByteArray,
        val imageUrl: String,
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
        private val logger = createLogger("FigmaAssetImporter")

        private fun <R> ApiResponse<R>.logError(message: () -> String) {
            if (this is ApiResponse.Failure) {
                logger.error(this.asException(), message)
            }
        }
    }
}
