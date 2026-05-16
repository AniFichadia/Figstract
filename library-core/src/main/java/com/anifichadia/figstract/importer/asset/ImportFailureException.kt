package com.anifichadia.figstract.importer.asset

class ImportFailureException(val failedFiles: List<String>) : Exception(
    buildString {
        appendLine("Import completed with failures in ${failedFiles.size} file(s):")
        failedFiles.forEach { appendLine("  - $it") }
        append("Check the import report for details.")
    }
)
