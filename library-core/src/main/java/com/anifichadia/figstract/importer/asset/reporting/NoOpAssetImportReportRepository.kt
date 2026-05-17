package com.anifichadia.figstract.importer.asset.reporting

class NoOpAssetImportReportRepository : AssetImportReportRepository {
    override suspend fun save(report: FigmaImportReport) = Unit
}
