package com.anifichadia.figstract.importer.asset.reporting

import com.anifichadia.figstract.importer.asset.model.ImportResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ImportReportDocument(
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
        val nodeId: String,
        val imageUrl: String,
        val instruction: String,
    )

    @Serializable
    sealed class FailureEntry {
        abstract val nodeId: String
        abstract val reason: String
        abstract val cause: String?

        @Serializable
        @SerialName("get_images_failed")
        data class GetImagesFailed(
            override val nodeId: String,
            override val reason: String,
            override val cause: String?,
        ) : FailureEntry()

        @Serializable
        @SerialName("no_image_url")
        data class NoImageUrl(
            override val nodeId: String,
            override val reason: String,
            override val cause: String? = null,
        ) : FailureEntry()

        @Serializable
        @SerialName("download_failed")
        data class DownloadFailed(
            override val nodeId: String,
            override val reason: String,
            override val cause: String?,
            val imageUrl: String,
        ) : FailureEntry()

        @Serializable
        @SerialName("import_pipeline_failed")
        data class ImportPipelineFailed(
            override val nodeId: String,
            override val reason: String,
            override val cause: String?,
            val instruction: String,
        ) : FailureEntry()
    }

    companion object {
        fun from(report: FigmaImportReport, generatedAt: Instant): ImportReportDocument {
            val successes = report.successes()
            val failures = report.failures()

            return ImportReportDocument(
                figmaFile = report.figmaFile,
                generatedAt = generatedAt,
                summary = Summary(
                    total = successes.size + failures.size,
                    successes = successes.size,
                    failures = failures.size,
                ),
                successes = successes
                    .sortedBy { it.nodeId }
                    .map { s ->
                        SuccessEntry(
                            nodeId = s.nodeId,
                            imageUrl = s.imageUrl,
                            instruction = s.instruction.toString(),
                        )
                    },
                failures = failures
                    .sortedBy { it.nodeId }
                    .map { f ->
                        when (f) {
                            is ImportResult.Failure.GetImagesFailed -> FailureEntry.GetImagesFailed(
                                nodeId = f.nodeId,
                                reason = f.reason,
                                cause = f.cause.message,
                            )

                            is ImportResult.Failure.NoImageUrl -> FailureEntry.NoImageUrl(
                                nodeId = f.nodeId,
                                reason = f.reason,
                            )

                            is ImportResult.Failure.DownloadFailed -> FailureEntry.DownloadFailed(
                                nodeId = f.nodeId,
                                reason = f.reason,
                                cause = f.cause.message,
                                imageUrl = f.imageUrl,
                            )

                            is ImportResult.Failure.ImportPipelineFailed -> FailureEntry.ImportPipelineFailed(
                                nodeId = f.nodeId,
                                reason = f.reason,
                                cause = f.cause.message,
                                instruction = f.instruction.toString(),
                            )
                        }
                    },
            )
        }
    }
}
