package com.anifichadia.figstract.importer.asset.reporting

import com.anifichadia.figstract.importer.asset.model.ImportResult
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Thread-safe accumulator for [ImportResult]s associated with a single Figma file.
 */
class FigmaImportReport(val figmaFile: String) {
    private val results = ConcurrentLinkedQueue<ImportResult>()

    fun record(result: ImportResult) {
        results.add(result)
    }

    fun results(): List<ImportResult> = results.toList()

    fun successes(): List<ImportResult.Success> =
        results.filterIsInstance<ImportResult.Success>()

    fun failures(): List<ImportResult.Failure> =
        results.filterIsInstance<ImportResult.Failure>()

    fun hasFailures(): Boolean = results.any { it is ImportResult.Failure }

    fun summary(): String = buildString {
        val allResults = results()
        val successes = allResults.filterIsInstance<ImportResult.Success>()
        val failures = allResults.filterIsInstance<ImportResult.Failure>()
        appendLine("=== Import Report: $figmaFile ===")
        appendLine("Total:    ${allResults.size}")
        appendLine("Success:  ${successes.size}")
        appendLine("Failures: ${failures.size}")
    }
}
