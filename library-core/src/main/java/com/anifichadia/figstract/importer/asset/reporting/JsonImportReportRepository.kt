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

        val nameSegment = report.name?.let { "${it}_" }.orEmpty()
        val reportFile = outputDir.resolve("$FILE_PREFIX${report.figmaFile}_$nameSegment$timestamp.json")

        val document = ImportReportDocument.from(
            report = report,
            generatedAt = generatedAt,
        )
        reportFile.writeText(json.encodeToString(document))

        logger.info { "Report written to: ${reportFile.absolutePath}" }
    }

    override suspend fun findLatest(figmaFile: String, name: String?): ImportReportDocument? {
        // The name segment isn't parsed out of the filename since it sits ambiguously alongside the timestamp.
        val candidates = outputDir.listFiles { file ->
            file.isFile && file.name.startsWith("$FILE_PREFIX${figmaFile}_") && file.name.endsWith(".json")
        }

        if (candidates.isNullOrEmpty()) return null

        return candidates
            .mapNotNull { file ->
                try {
                    json.decodeFromString<ImportReportDocument>(file.readText())
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to parse import report, ignoring: ${file.absolutePath}" }
                    null
                }
            }
            .filter { it.name == name }
            .maxByOrNull { it.generatedAt }
    }

    companion object {
        private const val FILE_PREFIX = "import_report_"

        private val logger = createLogger("JsonFileImportReportRepository")

        val DefaultJson = Json {
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
