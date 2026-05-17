package com.anifichadia.figstract.importer.variable.reporting

import com.anifichadia.figstract.importer.variable.model.VariableImportResult
import java.util.concurrent.ConcurrentLinkedQueue

class VariableImportReport(val figmaFile: String) {
    private val results = ConcurrentLinkedQueue<VariableImportResult>()

    fun record(result: VariableImportResult) {
        results.add(result)
    }

    fun results(): List<VariableImportResult> = results.toList()

    fun successes(): List<VariableImportResult.Success> = results.filterIsInstance<VariableImportResult.Success>()

    fun failures(): List<VariableImportResult.Failure> = results.filterIsInstance<VariableImportResult.Failure>()

    fun hasFailures(): Boolean = results.any { it is VariableImportResult.Failure }

    fun summary(): String = buildString {
        val allResults = results()
        val successes = allResults.filterIsInstance<VariableImportResult.Success>()
        val failures = allResults.filterIsInstance<VariableImportResult.Failure>()
        appendLine("=== Variable Import Report: $figmaFile ===")
        appendLine("Total:    ${allResults.size}")
        appendLine("Success:  ${successes.size}")
        appendLine("Failures: ${failures.size}")
    }
}
