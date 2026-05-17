package com.anifichadia.figstract.importer.variable.reporting

import com.anifichadia.figstract.util.createLogger
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.time.Clock

class JsonFileVariableImportReportRepository(
    private val outputDir: File,
    private val json: Json = DefaultJson,
) : VariableImportReportRepository {
    init {
        outputDir.mkdirs()
    }

    override suspend fun save(report: VariableImportReport) {
        val generatedAt = Clock.System.now()
        val timestamp = generatedAt.toString().replace(':', '-')
        val reportFile = outputDir.resolve("variable_import_report_${report.figmaFile}_$timestamp.json")

        val document = VariableImportReportDocument.from(report, generatedAt)
        reportFile.writeText(json.encodeToString(document))

        logger.info { "Report written to: ${reportFile.absolutePath}" }
    }

    companion object {
        private val logger = createLogger("JsonFileVariableImportReportRepository")

        val DefaultJson = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
