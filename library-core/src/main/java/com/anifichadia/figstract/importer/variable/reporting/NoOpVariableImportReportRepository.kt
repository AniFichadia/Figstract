package com.anifichadia.figstract.importer.variable.reporting

class NoOpVariableImportReportRepository : VariableImportReportRepository {
    override suspend fun save(report: VariableImportReport) = Unit
}
