package com.anifichadia.figmaimporter.models.importer.importing

import com.anifichadia.figmaimporter.models.importer.Instruction
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import java.io.File

interface Destination : ImportPipelineStep {
    override suspend fun process(
        instruction: Instruction,
        input: ImportPipelineStep.Output,
    ): ImportPipelineStep.Output {
        write(instruction, input)

        // Finalising
        return ImportPipelineStep.Output(ByteReadChannel.Empty, input.format)
    }

    suspend fun write(
        instruction: Instruction,
        input: ImportPipelineStep.Output,
    )

    data object None : Destination {
        override suspend fun write(instruction: Instruction, input: ImportPipelineStep.Output) {
            // No-op
        }
    }

    class Directory(
        private val destinationDirectory: File,
    ): Destination {
        init { require(destinationDirectory.isDirectory) { "destinationDirectory '$destinationDirectory' must be a directory" } }

        override suspend fun write(
            instruction: Instruction,
            input: ImportPipelineStep.Output,
        ) {
            val channel = input.channel
            val format = input.format

            val extension = format ?: instruction.export.config.format.fileExtension
            val outputFile = File(destinationDirectory, "${instruction.import.outputName}.${extension}")

            channel.copyAndClose(outputFile.writeChannel())
        }
    }

    companion object {
        fun directory(directory: File): Directory {
            return Directory(
                directory.also { it.mkdirs() },
            )
        }
    }
}
