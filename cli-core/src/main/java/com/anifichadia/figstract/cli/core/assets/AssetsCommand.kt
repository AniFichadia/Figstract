package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.HttpClientFactory
import com.anifichadia.figstract.cli.core.ProcessingRecordOptionGroup
import com.anifichadia.figstract.cli.core.defaultJsonValueSource
import com.anifichadia.figstract.cli.core.defaultPropertyValueSource
import com.anifichadia.figstract.cli.core.getFigmaApi
import com.anifichadia.figstract.cli.core.getProxy
import com.anifichadia.figstract.cli.core.outDirectory
import com.anifichadia.figstract.importer.asset.FigmaAssetImporter
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.reporting.JsonFileAssetImportReportRepository
import com.anifichadia.figstract.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figstract.model.tracking.NoOpProcessingRecordRepository
import com.anifichadia.figstract.type.fold
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import kotlinx.coroutines.coroutineScope
import java.io.File

abstract class AssetsCommand : SuspendingCliktCommand(
    name = "assets",
) {
    override val printHelpOnEmptyArgs = true

    init {
        context {
            valueSources(
                defaultPropertyValueSource(),
                defaultJsonValueSource(),
            )
        }
    }

    private val proxyConfig by getProxy()
    private val figmaApi by getFigmaApi()

    private val processingRecordOptions by ProcessingRecordOptionGroup()

    private val outDirectory by outDirectory()

    override fun help(context: Context): String {
        return """
        Extracts assets from figma files, such as images and icons
    """.trimIndent()
    }

    abstract fun createHandlers(outDirectory: File): List<AssetFileHandler>

    override suspend fun run() {
        val downloaderHttpClient = HttpClientFactory.downloader(
            proxy = proxyConfig,
        )

        val processingRecordRepository = if (processingRecordOptions.enabled) {
            val name = processingRecordOptions.name?.let { "processing_record_$it.json" } ?: "processing_record.json"
            JsonFileProcessingRecordRepository(
                recordFile = outDirectory.fold(name),
            )
        } else {
            NoOpProcessingRecordRepository
        }

        val importReportRepository = JsonFileAssetImportReportRepository(
            outputDir = outDirectory,
        )

        val importer = FigmaAssetImporter(
            figmaApi = figmaApi,
            downloaderHttpClient = downloaderHttpClient,
            processingRecordRepository = processingRecordRepository,
            importReportRepository = importReportRepository,
        )
        val handlers = createHandlers(outDirectory)
        if (handlers.isEmpty()) error("No handlers available.")

        coroutineScope {
            try {
                importer.importFromFigma(
                    handlers = handlers,
                )
            } catch (e: Throwable) {
                echo(e.message, err = true)
                throw ProgramResult(1)
            }
        }
    }
}
