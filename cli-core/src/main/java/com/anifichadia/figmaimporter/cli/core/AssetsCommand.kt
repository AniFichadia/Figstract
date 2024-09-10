package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.figma.api.FigmaApi
import com.anifichadia.figmaimporter.importer.asset.FigmaAssetImporter
import com.anifichadia.figmaimporter.importer.asset.model.AssetFileHandler
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
import com.anifichadia.figmaimporter.type.fold
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.sources.PropertiesValueSource
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File

abstract class AssetsCommand : CliktCommand(
    name = "assets",
    help = """
        Extracts assets from figma files, such as images and icons
    """.trimIndent(),
    printHelpOnEmptyArgs = true,
) {
    init {
        context {
            valueSources(
                PropertiesValueSource.from("$commandName.properties"),
            )
        }
    }

    private val proxyConfig by findObject<ProxyConfig>()
    private val figmaApi by requireObject<FigmaApi>()

    private val trackingEnabled: Boolean by option("--trackingEnabled")
        .boolean()
        .default(true)

    private val outPath: File by option("--out", "-o")
        .file(
            canBeFile = false,
            canBeDir = true,
        )
        .default(File("./out"))

    abstract fun createHandlers(outDirectory: File): List<AssetFileHandler>

    override fun run() = runBlocking {
        val downloaderHttpClient = HttpClientFactory.downloader(
            proxy = proxyConfig,
        )

        val outDirectory = outPath.fold("assets")

        val processingRecordRepository = if (trackingEnabled) {
            JsonFileProcessingRecordRepository(
                recordFile = File(outDirectory, "processing_record.json")
            )
        } else {
            NoOpProcessingRecordRepository
        }

        val importer = FigmaAssetImporter(
            figmaApi = figmaApi,
            downloaderHttpClient = downloaderHttpClient,
            processingRecordRepository = processingRecordRepository,
        )
        coroutineScope {
            importer.importFromFigma(
                handlers = createHandlers(outDirectory),
            )
        }
    }
}
