package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.FigmaImporter
import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
import kotlinx.coroutines.coroutineScope
import java.io.File

object CliHelper {
    suspend fun execute(
        authType: AuthType,
        authToken: String,
        proxyHost: String?,
        proxyPort: Int?,
        trackingEnabled: Boolean,
        outDirectory: File,
        createHandlers: HandlerCreator,
    ) {
        val authProvider = authType.createAuthProvider(authToken)
        val proxy = getProxyConfig(proxyHost, proxyPort)

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

        val importer = FigmaImporter(
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
        fun create(outDirectory: File): List<FigmaFileHandler>
    }
}
