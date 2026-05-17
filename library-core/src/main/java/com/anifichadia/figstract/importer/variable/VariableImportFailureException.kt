package com.anifichadia.figstract.importer.variable

class VariableImportFailureException(val failedFiles: List<String>) : Exception(
    buildString {
        appendLine("Variable import completed with failures in ${failedFiles.size} file(s):")
        failedFiles.forEach { appendLine("  - $it") }
        append("Check the variable import reports for details.")
    }
)
