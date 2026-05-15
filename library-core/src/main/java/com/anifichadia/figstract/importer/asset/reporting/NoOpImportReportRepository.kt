package com.anifichadia.figstract.importer.asset.reporting

class NoOpImportReportRepository : ImportReportRepository {
    override suspend fun save(report: FigmaImportReport) = Unit
}
