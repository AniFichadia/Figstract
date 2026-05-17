package com.anifichadia.figstract.importer.variable.reporting

import com.anifichadia.figstract.importer.variable.model.VariableImportResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class VariableImportReportDocument(
    val figmaFile: String,
    val generatedAt: Instant,
    val summary: Summary,
    val failures: List<FailureEntry>,
    val successes: List<SuccessEntry>,
) {
    @Serializable
    data class Summary(
        val total: Int,
        val successes: Int,
        val failures: Int,
    )

    @Serializable
    data class SuccessEntry(
        val variableCollectionName: String,
        val writerName: String,
    )

    @Serializable
    sealed class FailureEntry {
        abstract val reason: String
        abstract val cause: String?

        @Serializable
        @SerialName("get_variables_failed")
        data class GetVariablesFailed(
            override val reason: String,
            override val cause: String?,
        ) : FailureEntry()

        @Serializable
        @SerialName("no_metadata")
        data class NoMetadata(
            override val reason: String,
            override val cause: String? = null,
        ) : FailureEntry()

        @Serializable
        @SerialName("write_failed")
        data class WriteFailed(
            val variableCollectionName: String,
            val writerName: String,
            override val reason: String,
            override val cause: String?,
        ) : FailureEntry()
    }

    companion object {
        fun from(report: VariableImportReport, generatedAt: Instant): VariableImportReportDocument {
            val successes = report.successes()
            val failures = report.failures()

            return VariableImportReportDocument(
                figmaFile = report.figmaFile,
                generatedAt = generatedAt,
                summary = Summary(
                    total = successes.size + failures.size,
                    successes = successes.size,
                    failures = failures.size,
                ),
                successes = successes
                    .sortedWith(compareBy({ it.variableCollectionName }, { it.writerName }))
                    .map { s ->
                        SuccessEntry(
                            variableCollectionName = s.variableCollectionName,
                            writerName = s.writerName,
                        )
                    },
                failures = failures
                    .sortedWith(compareBy(
                        // File-level failures first
                        { it !is VariableImportResult.Failure.GetVariablesFailed && it !is VariableImportResult.Failure.NoMetadata },
                        { (it as? VariableImportResult.Failure.WriteFailed)?.variableCollectionName ?: "" },
                        { (it as? VariableImportResult.Failure.WriteFailed)?.writerName ?: "" },
                    ))
                    .map { f ->
                        when (f) {
                            is VariableImportResult.Failure.GetVariablesFailed -> FailureEntry.GetVariablesFailed(
                                reason = f.reason,
                                cause = f.cause.message,
                            )
                            is VariableImportResult.Failure.NoMetadata -> FailureEntry.NoMetadata(
                                reason = f.reason,
                            )
                            is VariableImportResult.Failure.WriteFailed -> FailureEntry.WriteFailed(
                                variableCollectionName = f.variableCollectionName,
                                writerName = f.writerName,
                                reason = f.reason,
                                cause = f.cause.message,
                            )
                        }
                    },
            )
        }
    }
}
