package com.anifichadia.figstract.importer.asset.reporting

interface AssetImportReportRepository {
    suspend fun save(report: FigmaImportReport)

    suspend fun findLatest(figmaFile: String, name: String?): ImportReportDocument?
}
