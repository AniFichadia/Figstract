package com.anifichadia.figstract.importer.variable.model.variableorganization

/**
 * Drops the first path segment (the type-category prefix designers conventionally add, e.g. `colour/`, `spacing/`,
 * `typography/`)
 *
 * `nested = false`: `colour/Primary/Primary` → `["Primary/Primary"]`
 * `nested = true`:  `colour/Primary/Primary` → `["Primary", "Primary"]`
 */
data class StripRootVariableOrganizationStrategy(val nested: Boolean = false) : VariableOrganizationStrategy {
    override fun rewrite(figmaPath: String): List<String>? {
        val remainder = figmaPath.substringAfter(VariableOrganizationStrategy.DELIMITER, missingDelimiterValue = "")
        if (remainder.isEmpty()) return null
        return remainder.toSegments(nested)
    }
}
