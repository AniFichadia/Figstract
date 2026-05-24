package com.anifichadia.figstract.importer.variable.model.variableorganization

/**
 * No rewriting
 *
 * `nested = false`: `colour/Primary/Primary` → `["colour/Primary/Primary"]`
 * `nested = true`:  `colour/Primary/Primary` → `["colour", "Primary", "Primary"]`
 */
data class FullPathVariableOrganizationStrategy(val nested: Boolean = false) : VariableOrganizationStrategy {
    override fun rewrite(figmaPath: String): List<String> = figmaPath.toSegments(nested)
}
