package com.anifichadia.figmaimporter

import com.anifichadia.figmaimporter.models.figma.FigmaApi
import com.anifichadia.figmaimporter.models.importer.FigmaFileHandler
import com.anifichadia.figmaimporter.models.importer.Instruction
import com.anifichadia.figmaimporter.models.importer.exporting.ExportConfig
import com.anifichadia.figmaimporter.models.importer.importing.ImportPipelineStep.Companion.invoke
import com.anifichadia.figmaimporter.models.importer.importing.ImportPipelineStep.Companion.merge
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flattenConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlin.math.ceil
import kotlin.math.min

object FigmaImporter {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun importFromFigma(
        scope: CoroutineScope,
        figmaApi: FigmaApi,
        downloaderHttpClient: HttpClient,
        handlers: List<FigmaFileHandler>,
        assetsPerChunk: Int = MAX_ASSETS_PER_CHUNK,
    ) {
        require(assetsPerChunk <= MAX_ASSETS_PER_CHUNK) { "assetsPerChunk '$assetsPerChunk' exceeds $MAX_ASSETS_PER_CHUNK" }

        //region Exporting
        val fileFlow = handlers
            .asFlow()
            .flowOn(Dispatchers.Default)
            .map { handler ->
                println("Fetching ${handler.figmaFile}: Start")
                val getFileApiResponse = figmaApi.getFile(
                    key = handler.figmaFile,
                )
                println("Fetching ${handler.figmaFile}: Finish")

                handler to getFileApiResponse
            }
            .flowOn(Dispatchers.IO)
            // TODO: error handling
            .filter { (_, apiResponse) -> apiResponse.isSuccess() }
            .flowOn(Dispatchers.Default)

        val fileImportChunkFlow = fileFlow
            .map { (handler, getFileApiResponse) ->
                val response = getFileApiResponse.successBodyOrThrow()
                val exportInstructions = handler.extractor.extract(response)

                handler to exportInstructions
            }
            .map { (handler, exportInstructions) ->
                val exportsByConfig = exportInstructions.groupBy { it.export.config }
                handler to exportsByConfig
            }
            .flatMapConcat { (handler, exportsByConfig) ->
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
            .flowOn(Dispatchers.Default)

        val exportFlow = fileImportChunkFlow
            .flatMapMerge { (handler, exportConfig, chunkIndex, instructions) ->
                flow {
                    println("Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, chunkSize: ${instructions.size}, $exportConfig: Started")

                    val getImageResponseApiResponse = figmaApi.getImages(
                        key = handler.figmaFile,
                        ids = instructions.map { it.export.nodeId },
                        format = exportConfig.format,
                        scale = exportConfig.scale,
                    )
                    emit(getImageResponseApiResponse)

                    println("Getting images ${handler.figmaFile}, chunkIndex: $chunkIndex, chunkSize: ${instructions.size}, $exportConfig: ${getImageResponseApiResponse.isSuccess()}")
                }
                    // TODO: error handling
                    .filter { apiResponse -> apiResponse.isSuccess() }
                    .flowOn(Dispatchers.IO)
                    .map { response ->
                        val body = response.successBodyOrThrow()
                        if (body.images.keys != instructions.map { it.export.nodeId }.toSet()) {
                            error("Not all images retrieved")
                        }
                        body.images.entries.asFlow()
                            .mapNotNull { (nodeId, imageUrl) ->
                                val instruction = instructions.first { it.export.nodeId == nodeId }

                                try {
                                    val imageResponse = downloaderHttpClient.get(
                                        urlString = imageUrl,
                                    )
                                    val channel = imageResponse.bodyAsChannel()
                                    instruction to channel
                                } catch (e: Throwable) {
                                    println(e.toString())
                                    // TODO: failure handling
                                    null
                                }
                            }
                            .flowOn(Dispatchers.IO)
                    }
                    .flattenConcat()
                    .map { (instruction, byteReadChannel) ->
                        ExportOutput(
                            handler = handler,
                            instruction = instruction,
                            channel = byteReadChannel,
                        )
                    }
                    .flowOn(Dispatchers.Default)
            }
        //endregion

        //region Importing
        val destinationProcessorFlow = exportFlow
            .map { (handler, instruction, channel) ->
                println("Downloading ${handler.figmaFile} $instruction: Started")
                try {
                    instruction.import
                        .pipelineSteps
                        .merge()
                        .invoke(instruction, channel)
                } catch (e: Throwable) {
                    // TODO: error handling
                }
                println("Downloading ${handler.figmaFile} $instruction: Finished")
            }
            .flowOn(Dispatchers.IO)
        //endregion

        destinationProcessorFlow.launchIn(scope)
    }

    private data class Chunk(
        val handler: FigmaFileHandler,
        val exportConfig: ExportConfig,
        val chunkIndex: Int,
        val instructions: List<Instruction>,
    )

    private data class ExportOutput(
        val handler: FigmaFileHandler,
        val instruction: Instruction,
        val channel: ByteReadChannel,
    )

    private const val MAX_ASSETS_PER_CHUNK = 10
}
