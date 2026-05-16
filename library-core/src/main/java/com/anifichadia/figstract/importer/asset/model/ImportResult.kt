package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.NodeId

/**
 * Represents the outcome of a single image import operation within the pipeline.
 * Collected per Figma file and written to a report at the end of each handler's flow.
 */
sealed interface ImportResult {
    val figmaFile: FileKey

    data class Success(
        override val figmaFile: FileKey,
        val nodeId: NodeId,
        val imageUrl: String,
        val instruction: Instruction,
    ) : ImportResult

    sealed interface Failure : ImportResult {
        val reason: String
        val cause: Throwable?

        data class GetFileFailed(
            override val figmaFile: FileKey,
            override val cause: Throwable,
        ) : Failure {
            override val reason: String = "Failed to retrieve file: ${cause.message}"
        }

        sealed interface NodeFailure : Failure {
            val nodeId: NodeId

            data class GetImagesFailed(
                override val figmaFile: FileKey,
                override val nodeId: NodeId,
                override val cause: Throwable,
            ) : NodeFailure {
                override val reason: String = "Failed to retrieve image URL: ${cause.message}"
            }

            data class NoImageUrl(
                override val figmaFile: FileKey,
                override val nodeId: NodeId,
            ) : NodeFailure {
                override val reason: String = "No image URL"
                override val cause: Throwable? = null
            }

            data class DownloadFailed(
                override val figmaFile: FileKey,
                override val nodeId: NodeId,
                val imageUrl: String,
                override val cause: Throwable,
            ) : NodeFailure {
                override val reason: String = "Failed to download image: ${cause.message}"
            }

            data class ImportPipelineFailed(
                override val figmaFile: FileKey,
                override val nodeId: NodeId,
                val instruction: Instruction,
                override val cause: Throwable,
            ) : NodeFailure {
                override val reason: String = "Import pipeline failed: ${cause.message}"
            }
        }
    }
}
