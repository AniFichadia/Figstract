package com.anifichadia.figstract.importer.variable.reporting

interface VariableImportReportRepository {
    suspend fun save(report: VariableImportReport)
}
