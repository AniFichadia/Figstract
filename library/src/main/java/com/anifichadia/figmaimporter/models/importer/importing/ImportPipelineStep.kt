package com.anifichadia.figmaimporter.models.importer.importing

import com.anifichadia.figmaimporter.models.importer.Instruction
import io.ktor.utils.io.ByteReadChannel

fun interface ImportPipelineStep {
    suspend fun process(
        instruction: Instruction,
        input: Output,
    ): Output

    data class Output(
        val channel: ByteReadChannel,
        val format: String?,
    )

    companion object {
        val passthrough = ImportPipelineStep { _, output -> output }

        fun sideEffect(block: (instruction: Instruction, output: Output) -> Unit) {
            ImportPipelineStep { instruction, output ->
                block(instruction, output)
                output
            }
        }

        suspend operator fun ImportPipelineStep.invoke(
            instruction: Instruction,
            channel: ByteReadChannel,
            format: String? = null,
        ): Output {
            return this.process(instruction, Output(channel, format))
        }

        suspend operator fun ImportPipelineStep.invoke(
            instruction: Instruction,
            input: Output,
        ): Output {
            return this.process(instruction, input)
        }

        infix fun ImportPipelineStep.then(next: ImportPipelineStep): ImportPipelineStep {
            val original = this

            return if (original === passthrough) {
                next
            } else if (next === passthrough) {
                original
            } else {
                ImportPipelineStep { importData, channel ->
                    val originalOutput = original.process(importData, channel)
                    val nextOutput = next.process(importData, originalOutput)

                    // Ensure any format changes are propagated from the original if not available in the next
                    if (nextOutput.format == null) {
                        nextOutput.copy(format = originalOutput.format)
                    } else {
                        nextOutput
                    }
                }
            }
        }

        fun List<ImportPipelineStep>.merge(): ImportPipelineStep {
            return when {
                isEmpty() -> passthrough
                size == 1 -> this.first()
                else -> this.fold(passthrough) { acc, a -> acc.then(a) }
            }
        }
    }
}
