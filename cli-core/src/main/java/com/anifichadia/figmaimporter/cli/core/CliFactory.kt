package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.FigmaImporter
import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
import com.anifichadia.figmaimporter.util.FileManagement
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.coroutineScope
import java.io.File

object CliFactory {
    fun createArgParser() = ArgParser("Figma importer")

    suspend fun createCli(
        args: Array<String>,
        parser: ArgParser = createArgParser(),
        createHandlers: HandlerCreator,
    ) {
        //region Arg management
        val authType by parser.option(ArgType.Choice<AuthType>(), fullName = "auth.type")
            .default(AuthType.AccessToken)
        val authToken by parser.option(ArgType.String, fullName = "auth")
            .required()

        val proxyHost by parser.option(ArgType.String, fullName = "proxy.host")
        val proxyPort by parser.option(ArgType.Int, fullName = "proxy.port")

        val trackingDisabled by parser.option(ArgType.Boolean, fullName = "tracking.disabled")
            .default(false)

        val outPath by parser.option(ArgType.String, fullName = "out", shortName = "o")
            .default("out")

        parser.parse(args)
        //endregion

        val authProvider = authType.createAuthProvider(authToken)
        val proxy = getProxyConfig(proxyHost, proxyPort)

        val outDirectory = FileManagement.outDirectory(outPath)

        val figmaHttpClient = HttpClientFactory.figma(proxy = proxy)
        val figmaApi = FigmaApiImpl(
            httpClient = figmaHttpClient,
            authProvider = authProvider,
        )
        val downloaderHttpClient = HttpClientFactory.downloader(proxy = proxy)
        val processingRecordRepository = if (!trackingDisabled) {
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
