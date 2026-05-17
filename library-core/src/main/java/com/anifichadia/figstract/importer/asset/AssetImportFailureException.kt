package com.anifichadia.figstract.importer.asset

class AssetImportFailureException(val failedFiles: List<String>) : Exception(
    buildString {
        appendLine("Asset import completed with failures in ${failedFiles.size} file(s):")
        failedFiles.forEach { appendLine("  - $it") }
        append("Check the asset import report for details.")
    }
)
