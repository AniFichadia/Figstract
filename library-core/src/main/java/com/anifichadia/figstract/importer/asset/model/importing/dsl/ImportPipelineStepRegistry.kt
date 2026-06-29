package com.anifichadia.figstract.importer.asset.model.importing.dsl

import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry.Companion.buildImportPipelineStepRegistry

/**
 * An immutable registry mapping step names to factory functions that produce [ImportPipeline.Step] instances from
 * parsed [StepExpression] params.
 *
 * [ImportPipelineStepRegistry]s are recommended to be built using [buildImportPipelineStepRegistry].
 *
 * Registries are composed using [plus], with the right-hand registry's entries winning on collision.
 *
 * ```kotlin
 * val registry = Registry1 + Registry2
 * val step = ImportPipelineDsl(registry).parse("androidSvgToAvd()")
 * ```
 */
class ImportPipelineStepRegistry(
    steps: Map<String, StepFactory>,
) {
    private val steps = steps.toMap()

    /**
     * Resolves [expression] to an [ImportPipeline.Step] using the registered factory.
     *
     * @throws ImportPipelineDslException if the step name is unknown or a parameter is invalid.
     */
    fun resolve(expression: StepExpression): ImportPipeline.Step {
        val factory = steps[expression.name]
            ?: throw ImportPipelineDslException(
                "Unknown pipeline step '${expression.name}'. Registered steps: ${registeredStepNames().joinToString()}",
            )
        return try {
            factory.create(StepParams(expression.params))
        } catch (e: ImportPipelineDslException) {
            throw e
        } catch (e: Exception) {
            throw ImportPipelineDslException(
                "Failed to create step '${expression.name}' with params ${expression.params}: ${e.message}",
                e,
            )
        }
    }

    fun registeredStepNames(): Set<String> = steps.keys.toSortedSet()

    /**
     * Combines the steps from multiple [ImportPipelineStepRegistry]. When the same step name exists in both, the entry
     * from [other] wins.
     */
    operator fun plus(other: ImportPipelineStepRegistry): ImportPipelineStepRegistry {
        return ImportPipelineStepRegistry(steps + other.steps)
    }

    class StepParams(original: Map<String, String>) : Map<String, String> by original {
        //region Values
        inline fun <reified T> value(
            key: String,
            noinline map: (String) -> T? = { it.fromString<T>(key) },
        ): T = valueOrNull(key, map) ?: throw ImportPipelineDslException("Missing parameter '$key' with no default")

        inline fun <reified T> valueOrNull(
            key: String,
            noinline map: (String) -> T? = { it.fromString<T>(key) },
        ): T? = this[key]?.let(map)

        inline fun <reified T> valueOrDefault(
            key: String,
            noinline map: (String) -> T? = { it.fromString<T>(key) },
            default: () -> T,
        ): T = valueOrNull(key, map) ?: default()
        //endregion

        //region Enums
        inline fun <reified T : Enum<T>> enum(
            key: String,
        ): T = enumOrNull<T>(key) ?: throw ImportPipelineDslException("Missing parameter '$key' with no default")

        inline fun <reified T : Enum<T>> enumOrNull(
            key: String,
        ): T? {
            val raw = this[key] ?: return null
            return raw.toEnum<T>()
                ?: throw ImportPipelineDslException("Parameter '$key' must be one of ${enumValues<T>().map { it.name }}, got '$raw'")
        }

        inline fun <reified T : Enum<T>> enumOrDefault(
            key: String,
            default: () -> T,
        ): T = enumOrNull<T>(key) ?: default()
        //endregion

        //region List
        inline fun <reified T> list(
            key: String,
            separator: String = ",",
            noinline map: (String) -> T? = { it.fromString<T>(key) },
        ): List<T> = listOrNull(key, separator, map)
            ?: throw ImportPipelineDslException("Missing parameter '$key' with no default")

        inline fun <reified T> listOrNull(
            key: String,
            separator: String = ",",
            noinline map: (String) -> T? = { it.fromString<T>(key) },
        ): List<T>? = this[key]
            ?.split(separator)
            ?.map { token ->
                val trimmed = token.trim()
                map(trimmed)
                    ?: throw ImportPipelineDslException("Could not convert '$trimmed' to ${T::class.simpleName} for parameter '$key'")
            }

        inline fun <reified T> listOrDefault(
            key: String,
            separator: String = ",",
            noinline map: (String) -> T? = { it.fromString<T>(key) },
            default: () -> List<T>,
        ): List<T> = listOrNull(key, separator, map) ?: default()
        //endregion

        //region Enum list
        inline fun <reified T : Enum<T>> enumList(
            key: String,
            separator: String = ",",
        ): List<T> = enumListOrNull<T>(key, separator)
            ?: throw ImportPipelineDslException("Missing parameter '$key' with no default")

        inline fun <reified T : Enum<T>> enumListOrNull(
            key: String,
            separator: String = ",",
        ): List<T>? = listOrNull(key, separator) { it.toEnum<T>() }

        inline fun <reified T : Enum<T>> enumListOrDefault(
            key: String,
            separator: String = ",",
            default: () -> List<T>,
        ): List<T> = enumListOrNull<T>(key, separator) ?: default()
        //endregion

        //region Converters
        inline fun <reified T : Enum<T>> String.toEnum(): T? {
            return enumValues<T>().firstOrNull { it.name == this }
        }

        private inline fun <reified T> String.fromString(key: String): T? = when (T::class) {
            String::class -> this
            Boolean::class -> toBooleanStrictOrNull()
                ?: throw ImportPipelineDslException("Parameter '$key' must be a boolean, got '$this'")

            Int::class -> toIntOrNull()
                ?: throw ImportPipelineDslException("Parameter '$key' must be an int, got '$this'")

            Long::class -> toLongOrNull()
                ?: throw ImportPipelineDslException("Parameter '$key' must be a long, got '$this'")

            Float::class -> toFloatOrNull()
                ?: throw ImportPipelineDslException("Parameter '$key' must be a float, got '$this'")

            Double::class -> toDoubleOrNull()
                ?: throw ImportPipelineDslException("Parameter '$key' must be a double, got '$this'")

            else -> null
        } as T?
        //endregion
    }

    /** Converts the params of a [StepExpression] into an [ImportPipeline.Step]. */
    fun interface StepFactory {
        fun create(params: StepParams): ImportPipeline.Step
    }

    class Builder {
        private val steps = mutableMapOf<String, StepFactory>()

        infix fun String.withFactory(factory: StepFactory) {
            steps[this] = factory
        }

        fun build() = ImportPipelineStepRegistry(steps.toMap())
    }

    companion object {
        fun buildImportPipelineStepRegistry(block: Builder.() -> Unit) = Builder().apply(block).build()
    }
}
