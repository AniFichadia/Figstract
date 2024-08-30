package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.anifichadia.figmaimporter.importer.asset.FigmaAssetImporter
import com.anifichadia.figmaimporter.importer.asset.model.AssetFileHandler
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File

abstract class AssetCommand : CliktCommand(name = "asset") {
    private val authProvider by requireObject<AuthProvider>()
    private val proxyConfig by findObject<ProxyConfig>()

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
        val figmaHttpClient = HttpClientFactory.figma(proxy = proxyConfig)
        val figmaApi = FigmaApiImpl(
            httpClient = figmaHttpClient,
            authProvider = authProvider,
        )
        val downloaderHttpClient = HttpClientFactory.downloader(proxy = proxyConfig)
        val processingRecordRepository = if (trackingEnabled) {
            JsonFileProcessingRecordRepository(
                recordFile = File(outPath, "processing_record.json")
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
