package com.anifichadia.figmaimporter.importer.asset.model.importing

import com.anifichadia.figmaimporter.importer.asset.model.Instruction
import com.anifichadia.figmaimporter.importer.asset.model.Instruction.ImportTarget.Companion.merge
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.passThrough
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.IfElse.Companion.otherwiseDefault
import com.anifichadia.figmaimporter.model.Describeable
import com.anifichadia.figmaimporter.model.Describeable.Companion.describeOrToString
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Used to manipulate, process or finalise assets retrieved from figma.
 */
data class ImportPipeline(
    val steps: Step = passThrough(),
    val destination: Destination,
    val before: suspend () -> Unit = {},
    val after: suspend (successful: Boolean) -> Unit = {},
) {
    suspend fun execute(
        instruction: Instruction,
        data: ByteArray,
        outputName: String? = null,
        outputPathElements: List<String> = emptyList(),
        format: String? = null,
    ) {
        try {
            before()

            (steps then destination).process(
                instruction = instruction,
                input = Output(
                    data = data,
                    target = Instruction.ImportTarget.Override(
                        outputName = outputName,
                        pathElements = outputPathElements,
                        format = format,
                    ),
                )
            )

            after(true)
        } catch (e: Throwable) {
            after(false)

            throw e
        }
    }

    /**
     * Processes asset from figma. Each [Step] produces a list of [Output]s which describe the outcome of a [Step].
     *
     * [Step]s may be run asynchronously and are suspending, so if there's any non-concurrent operation (e.g. writing to
     * a common file) must use locks or flow control mechanisms within the implementation. Consider using a
     * [kotlinx.coroutines.sync.Semaphore] or [kotlinx.coroutines.sync.Mutex]?
     *
     * [Step]s can produce 0 or more [Output]:
     *  - 0 [Output]s may indicate a terminal [Step] in a processing pipeline, or an error, but it's possibly preferable
     *  to just throw an exception. Also see [Output.none]
     *  - Single [Output]s are pretty standard and the [Output.single] convenience function can be used to return a
     *  single [Output].
     *  - Producing multiple outputs allow fanning out subsequent [Step]s. For example, using a larger image and
     *  outputting multiple, downsized versions of it for further processing.
     */
    fun interface Step {
        /** When writing [Step]s, it's recommended to call [Output.copy] on [input] with any updated properties. */
        suspend fun process(
            instruction: Instruction,
            input: Output,
        ): List<Output>

        /**
         * A no-op. Can be used a default or initial value for defining [ImportPipeline].
         *
         * This may be skipped when creating [ImportPipeline]s by calling [then].
         */
        object PassThrough : Step, Describeable {
            override suspend fun process(instruction: Instruction, input: Output): List<Output> {
                return input.single()
            }

            override fun describe(): String {
                return "PassThrough"
            }

            override fun toString(): String {
                return describe()
            }
        }

        /**
         * Convenient way of having a [Step] with a side effect: something that performs external manipulation without
         * affecting the [Output]
         */
        abstract class SideEffect : Step, Describeable {
            abstract suspend fun perform(instruction: Instruction, input: Output)

            final override suspend fun process(
                instruction: Instruction,
                input: Output,
            ): List<Output> {
                perform(instruction, input)
                return PassThrough.process(instruction, input)
            }

            override fun describe(): String {
                return "SideEffect"
            }

            override fun toString(): String {
                return describe()
            }
        }

        class Then(
            private val first: Step,
            private val second: Step,
        ) : Step, Describeable {
            override suspend fun process(instruction: Instruction, input: Output): List<Output> {
                val firstOutput = first.process(instruction, input)
                val secondOutput = firstOutput.flatMap { firstInput ->
                    second.process(instruction, firstInput)
                }

                return secondOutput
            }

            override fun describe(): String {
                return "${first.describeOrToString()} THEN ${second.describeOrToString()}"
            }

            override fun toString(): String {
                return describe()
            }
        }

        class And(private val steps: List<Step>) : Step, Describeable {
            constructor(first: Step, second: Step) : this(listOf(first, second))

            override suspend fun process(instruction: Instruction, input: Output): List<Output> {
                val outputs = coroutineScope {
                    steps
                        .map { step ->
                            async {
                                step.process(instruction, input)
                            }
                        }
                        .awaitAll()
                }

                return outputs.flatten()
            }

            override fun describe(): String {
                return steps.joinToString(
                    separator = " AND ",
                    prefix = "(",
                    postfix = ")"
                ) { "(${it.describeOrToString()})" }
            }

            override fun toString(): String {
                return describe()
            }
        }

        class Or(private val steps: List<Step>) : Step, Describeable {
            constructor(first: Step, second: Step) : this(listOf(first, second))

            override suspend fun process(instruction: Instruction, input: Output): List<Output> {
                for (step in steps) {
                    val output = step.process(instruction, input)
                    if (output.isNotEmpty()) {
                        return output
                    }
                }

                return Output.none
            }

            override fun describe(): String {
                return steps.joinToString(
                    separator = " OR ",
                    prefix = "(",
                    postfix = ")"
                ) { "(${it.describeOrToString()})" }
            }

            override fun toString(): String {
                return describe()
            }
        }

        class IfElse(
            private val step: Step,
            private val otherwise: Step = otherwiseDefault,
            private val predicate: suspend (instruction: Instruction, input: Output) -> Boolean,
        ) : Step, Describeable {
            override suspend fun process(instruction: Instruction, input: Output): List<Output> {
                val stepToRun = if (predicate(instruction, input)) {
                    step
                } else {
                    otherwise
                }

                return stepToRun.process(instruction, input)
            }

            override fun describe(): String {
                return "IF $predicate THEN ${step.describeOrToString()} ELSE ${otherwise.describeOrToString()}"
            }

            override fun toString(): String {
                return describe()
            }

            companion object {
                val otherwiseDefault = passThrough()
            }
        }

        private class DescribeableStep(val description: String, val step: Step) : Step by step, Describeable {
            override fun describe(): String {
                return description
            }

            override fun toString(): String {
                return describe()
            }
        }

        companion object {
            operator fun invoke(description: String, step: Step): Step {
                return DescribeableStep(description, step)
            }

            //region Operators and creation helpers
            fun passThrough() = PassThrough

            fun sideEffect(
                description: String? = null,
                block: suspend (instruction: Instruction, input: Output) -> Unit,
            ): Step {
                return if (description != null) {
                    object : SideEffect() {
                        override suspend fun perform(instruction: Instruction, input: Output) {
                            block(instruction, input)
                        }

                        override fun describe(): String {
                            return "SideEffect: $description"
                        }
                    }
                } else {
                    object : SideEffect() {
                        override suspend fun perform(instruction: Instruction, input: Output) {
                            block(instruction, input)
                        }
                    }
                }
            }

            /**
             * Sequences two [Step] into one. If either is a [PassThrough], it may be discarded.
             *
             * All [Output]s from the first [Step] will be invoked and processed by [next] before proceeding.
             */
            infix fun Step.then(next: Step): Step {
                return resolve(this, next) { first, second ->
                    Then(first, second)
                }
            }

            /** @see [then] */
            fun List<Step>.then(): Step {
                return when {
                    isEmpty() -> passThrough()
                    size == 1 -> this.first()
                    else -> this.fold(PassThrough as Step) { acc, a -> acc.then(a) }
                }
            }

            infix fun Step.and(other: Step): Step {
                return resolve(this, other) { first, second ->
                    And(first, second)
                }
            }

            /** @see [and] */
            fun List<Step>.and(): Step {
                return when {
                    isEmpty() -> passThrough()
                    size == 1 -> this.first()
                    else -> And(this)
                }
            }

            infix fun Step.or(other: Step): Step {
                return resolve(this, other) { first, second ->
                    Or(first, second)
                }
            }

            /** @see [or] */
            fun List<Step>.or(): Step {
                return when {
                    isEmpty() -> passThrough()
                    size == 1 -> this.first()
                    else -> Or(this)
                }
            }

            fun Step.ifElse(
                step: Step,
                otherwise: Step = otherwiseDefault,
                predicate: suspend (instruction: Instruction, input: Output) -> Boolean,
            ): Step {
                return IfElse(step, otherwise, predicate)
            }

            private inline fun resolve(
                first: Step,
                second: Step,
                neither: (first: Step, second: Step) -> Step,
            ): Step {
                return if (first == PassThrough) {
                    second
                } else if (second == PassThrough) {
                    first
                } else {
                    neither(first, second)
                }
            }
            //endregion

            //region Value resolvers
            private fun merge(instruction: Instruction, input: Output): Instruction.ImportTarget.Initial {
                return instruction.import.importTarget.merge(input.target)
            }

            fun resolveOutputName(instruction: Instruction, input: Output): String {
                return merge(instruction, input).outputName
            }

            fun resolvePathElements(instruction: Instruction, input: Output): List<String> {
                return merge(instruction, input).pathElements
            }

            fun resolveExtension(instruction: Instruction, input: Output): String {
                return input.target.format ?: instruction.export.config.format.fileExtension
            }
            //endregion
        }
    }

    data class Output(
        val data: ByteArray,
        val target: Instruction.ImportTarget.Override,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Output) return false

            if (!data.contentEquals(other.data)) return false
            if (target != other.target) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + target.hashCode()
            return result
        }

        companion object {
            fun Output.single() = listOf(this)

            val none = emptyList<Output>()
        }
    }
}
