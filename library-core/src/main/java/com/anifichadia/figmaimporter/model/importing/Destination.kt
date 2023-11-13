package com.anifichadia.figmaimporter.model.importing

import com.anifichadia.figmaimporter.model.Describeable
import com.anifichadia.figmaimporter.model.Instruction
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolvePathElements
import java.io.File

/**
 * Used to save the output of an [ImportPipeline]. This is a specialised, finalising version of
 * [ImportPipeline.Step].
 */
abstract class Destination : ImportPipeline.Step {
    abstract suspend fun write(
        instruction: Instruction,
        input: ImportPipeline.Output,
    )

    final override suspend fun process(
        instruction: Instruction,
        input: ImportPipeline.Output,
    ): List<ImportPipeline.Output> {
        write(instruction, input)

        return ImportPipeline.Output.none
    }

    /** Black hole */
    object None : Destination(), Describeable {
        override suspend fun write(instruction: Instruction, input: ImportPipeline.Output) {
            // No-op
        }

        override fun describe(): String {
            return "Destination.none"
        }

        override fun toString(): String {
            return describe()
        }
    }

    /** @see [Destination.directoryDestination] */
    class Directory(
        private val directory: File,
    ) : Destination(), Describeable {
        override suspend fun write(
            instruction: Instruction,
            input: ImportPipeline.Output,
        ) {
            val data = input.data

            val outputName = resolveOutputName(instruction, input)
            val outputPath = resolvePathElements(instruction, input).joinToString(separator = File.separator)
            val extension = resolveExtension(instruction, input)

            val outputFile = File(directory, "$outputPath${File.separator}$outputName.$extension")
            outputFile.parentFile.mkdirs()

            outputFile.writeBytes(data)
        }

        override fun describe(): String {
            return "Destination.directory(directory: $directory)"
        }

        override fun toString(): String {
            return describe()
        }
    }

    companion object {
        fun directoryDestination(directory: File): Directory {
            return Directory(
                directory = directory,
            )
        }
    }
}
