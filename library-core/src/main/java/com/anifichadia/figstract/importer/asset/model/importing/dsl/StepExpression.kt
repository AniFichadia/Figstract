package com.anifichadia.figstract.importer.asset.model.importing.dsl

/**
 * Represents a parsed step call expression such as `convertToWebPLossy(qualityPercent=75)`.
 *
 * Parameter values are untyped strings at parse time; the [ImportPipelineStepRegistry] factory
 * functions are responsible for converting them to the correct types.
 */
data class StepExpression(
    val name: String,
    val params: Map<String, String>,
) {
    companion object {
        private val STEP_REGEX = Regex("""^(\w+)\s*(?:\(([^)]*)\))?$""")
        private val PARAM_REGEX = Regex("""(\w+)\s*=\s*("(?:[^"\\]|\\.)*"|[^,)]+)""")

        /**
         * Parses a single step expression string into a [StepExpression].
         *
         * Examples:
         * - `passThrough()`
         * - `convertToWebPLossy(qualityPercent=75)`
         * - `scale(scale=2.0)`
         * - `rename(name=myAsset)`
         */
        fun parse(raw: String): StepExpression {
            val trimmed = raw.trim()
            val match = STEP_REGEX.matchEntire(trimmed)
                ?: throw ImportPipelineDslException("Invalid step expression: '$trimmed'")

            val name = match.groupValues[1]
            val paramsRaw = match.groupValues[2].trim()

            val params = if (paramsRaw.isEmpty()) {
                emptyMap()
            } else {
                PARAM_REGEX.findAll(paramsRaw).associate { paramMatch ->
                    val key = paramMatch.groupValues[1]
                    val value = paramMatch.groupValues[2].trim().removeSurrounding("\"")
                    key to value
                }
            }

            return StepExpression(name = name, params = params)
        }
    }
}
