package com.anifichadia.figmaimporter.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object FileManagement {
    fun outDirectory(outPath: String): File {
        return Paths.get("", outPath).toFile()
    }

    private fun tempDirectoryPath(): Path {
        return Paths.get("", "temp").also {
            it.toFile().apply {
                mkdirs()
                deleteOnExit()
            }
        }
    }

    fun stepCreateTempFile(
        stepName: String,
        prefix: String? = null,
        suffix: String? = null,
    ): Path {
        return Files.createTempFile(
            /* dir = */ stepTempDirectoryPath(stepName),
            /* prefix = */ prefix,
            /* suffix = */ suffix,
        )
    }

    private fun stepTempDirectoryPath(stepName: String): Path {
        return tempDirectoryPath().resolve(stepName).also {
            it.toFile().apply {
                mkdirs()
                deleteOnExit()
            }
        }
    }
}
