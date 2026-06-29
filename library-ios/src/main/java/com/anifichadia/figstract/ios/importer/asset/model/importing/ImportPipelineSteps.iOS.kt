package com.anifichadia.figstract.ios.importer.asset.model.importing

import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figstract.util.FileManagement

fun convertToHeic(qualityPercent: Int = HEIC_LOSSY_QUALITY_PERCENT_DEFAULT): ImportPipeline.Step {
    require(qualityPercent in (0..100)) { "qualityPercent must be between 0 and 100" }

    return ImportPipeline.Step("convertToHeic(qualityPercent=$qualityPercent)") { instruction, input ->
        val prefix = "${instruction.import.importTarget.outputName}_"
        val tempInputFile = FileManagement.stepCreateTempFile(
            stepName = "convertToHeic",
            prefix = prefix,
            suffix = ".png",
        )
        val tempOutputFile = FileManagement.stepCreateTempFile(
            stepName = "convertToHeic",
            prefix = prefix,
            suffix = ".heic",
        )

        try {
            tempInputFile.toFile().writeBytes(input.data)

            val process = ProcessBuilder(
                "magick",
                tempInputFile.toAbsolutePath().toString(),
                "-quality", qualityPercent.toString(),
                tempOutputFile.toAbsolutePath().toString(),
            )
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            check(exitCode == 0) {
                "ImageMagick HEIC conversion failed (exit code $exitCode): $output"
            }

            input
                .copy(
                    data = tempOutputFile.toFile().readBytes(),
                    target = input.target.copy(
                        format = "heic",
                    ),
                )
                .single()
        } finally {
            tempInputFile.toFile().delete()
            tempOutputFile.toFile().delete()
        }
    }
}

const val HEIC_LOSSY_QUALITY_PERCENT_DEFAULT = 75
