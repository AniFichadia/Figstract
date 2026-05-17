package com.anifichadia.figstract.importer.asset.reporting

import com.anifichadia.figstract.util.createLogger
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock

class JsonFileAssetImportReportRepository(
    private val outputDir: File,
    private val json: Json = DefaultJson,
) : AssetImportReportRepository {

    init {
        outputDir.mkdirs()
    }

    override suspend fun save(report: FigmaImportReport) {
        val generatedAt = Clock.System.now()
        val timestamp = generatedAt.toString().replace(':', '-')

        val reportFile = outputDir.resolve("import_report_${report.figmaFile}_$timestamp.json")

        val document = ImportReportDocument.from(
            report = report,
            generatedAt = generatedAt,
        )
        reportFile.writeText(json.encodeToString(document))

        logger.info { "Report written to: ${reportFile.absolutePath}" }
    }

    companion object {
        private val logger = createLogger("JsonFileImportReportRepository")

        val DefaultJson = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
