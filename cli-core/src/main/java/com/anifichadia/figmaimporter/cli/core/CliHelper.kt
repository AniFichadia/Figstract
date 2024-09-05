package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.anifichadia.figmaimporter.importer.asset.FigmaAssetImporter
import com.anifichadia.figmaimporter.importer.asset.model.AssetFileHandler
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.coroutineScope
import java.io.File

object CliHelper {
    suspend fun execute(
        authProvider: AuthProvider,
        proxy: ProxyConfig?,
        trackingEnabled: Boolean,
        outDirectory: File,
        createHandlers: HandlerCreator,
    ) {
        val figmaHttpClient = HttpClientFactory.figma(proxy = proxy)
        val figmaApi = FigmaApiImpl(
            httpClient = figmaHttpClient,
            authProvider = authProvider,
        )
        val downloaderHttpClient = HttpClientFactory.downloader(proxy = proxy)
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
                handlers = createHandlers.create(outDirectory),
            )
        }
    }

    fun interface HandlerCreator {
        fun create(outDirectory: File): List<AssetFileHandler>
    }
}
