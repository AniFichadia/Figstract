package com.anifichadia.figstract.importer.asset.reporting

class NoOpAssetImportReportRepository : AssetImportReportRepository {
    override suspend fun save(report: FigmaImportReport) = Unit

    override suspend fun findLatest(figmaFile: String, name: String?): ImportReportDocument? = null
}
