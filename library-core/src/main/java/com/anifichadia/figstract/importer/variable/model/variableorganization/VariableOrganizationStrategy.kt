package com.anifichadia.figstract.importer.variable.model.variableorganization

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Controls how raw Figma variable paths are rewritten and segmented.
 *
 * [rewrite] returns a [List<String>] of path segments which are used to build the group hierarchy: multiple segments
 * produce nested groups, a single segment produces a flat leaf in the current group.
 */
interface VariableOrganizationStrategy {
    /**
     * Rewrite [figmaPath] into a list of path segments used to build the group hierarchy.
     * @return Return `null` to signal that rewriting failed and to fallback.
     */
    fun rewrite(figmaPath: String): List<String>?

    companion object {
        val Default = FullPathVariableOrganizationStrategy()

        const val DELIMITER = '/'
    }
}

fun String.toSegments(nested: Boolean): List<String> = if (nested) {
    split(VariableOrganizationStrategy.DELIMITER)
} else {
    listOf(this)
}

/**
 * Applies this strategy to [figmaPath], falling back to [VariableOrganizationStrategy.Default] with a warning if the
 * strategy returns null (i.e. the pattern did not match or the path was too short to apply the strategy).
 */
fun VariableOrganizationStrategy.rewriteWithFallback(figmaPath: String): List<String> {
    val result = rewrite(figmaPath)
    if (result == null) {
        logger.warn {
            "VariableOrganizationStrategy ${this::class.simpleName} did not match \"$figmaPath\" — falling back to Default"
        }
    }
    return result ?: VariableOrganizationStrategy.Default.rewrite(figmaPath)
}
