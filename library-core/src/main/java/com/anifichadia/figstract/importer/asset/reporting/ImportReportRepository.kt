package com.anifichadia.figstract.importer.asset.reporting

interface ImportReportRepository {
    suspend fun save(report: FigmaImportReport)
}
