package com.anifichadia.figstract.importer.variable.model.variableorganization

/**
 * User-supplied Kotlin regex with a replacement string, applied to the raw Figma path.
 *
 * Examples:
 * ```
 * // Strip a known root prefix, flat output
 * Custom(Regex("""^colour/(.+)$"""), "$1", nested = false)
 *
 * // Strip root and nest
 * Custom(Regex("""^colour/(.+)$"""), "$1", nested = true)
 *
 * // Collapse duplicate leaf: "Primary/Primary" → "Primary"
 * Custom(Regex("""^(.*)/([^/]+)/\2$"""), "$1/$2", nested = true)
 * ```
 */
data class CustomRegexVariableOrganizationStrategy(
    val pattern: Regex,
    val replacement: String,
    val nested: Boolean = false,
) : VariableOrganizationStrategy {
    override fun rewrite(figmaPath: String): List<String>? {
        if (!pattern.containsMatchIn(figmaPath)) return null
        return pattern.replace(figmaPath, replacement).toSegments(nested)
    }
}
