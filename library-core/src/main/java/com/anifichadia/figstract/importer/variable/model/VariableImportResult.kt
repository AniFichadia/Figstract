package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.FileKey

sealed interface VariableImportResult {
    val figmaFile: FileKey

    data class Success(
        override val figmaFile: FileKey,
        val variableCollectionName: String,
        val writerName: String,
    ) : VariableImportResult

    sealed interface Failure : VariableImportResult {
        val reason: String
        val cause: Throwable?

        data class GetVariablesFailed(
            override val figmaFile: FileKey,
            override val cause: Throwable,
        ) : Failure {
            override val reason: String = "Failed to retrieve variables: ${cause.message}"
        }

        data class NoMetadata(
            override val figmaFile: FileKey,
        ) : Failure {
            override val reason: String = "Response contained no metadata"
            override val cause: Throwable? = null
        }

        data class WriteFailed(
            override val figmaFile: FileKey,
            val variableCollectionName: String,
            val writerName: String,
            override val cause: Throwable,
        ) : Failure {
            override val reason: String = "Writer failed: ${cause.message}"
        }
    }
}
