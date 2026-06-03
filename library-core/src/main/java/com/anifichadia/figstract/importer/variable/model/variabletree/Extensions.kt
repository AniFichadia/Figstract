package com.anifichadia.figstract.importer.variable.model.variabletree

/**
 * Returns true if this group or any descendant has non-color variables.
 * Used by writers that skip color buckets (e.g. SwiftUI) to prune empty subtrees.
 */
fun VariableGroup.hasNonColorVariables(): Boolean {
    val hasNonColorBucket = buckets.any {
        it !is VariableTypeBucket.Single.Colors && it !is VariableTypeBucket.LightAndDark.Colors
    }
    return hasNonColorBucket || children.any { it.hasNonColorVariables() }
}

fun VariableGroup.toDebugString(indent: Int = 0): String = buildString {
    val prefix = "  ".repeat(indent)
    appendLine("${prefix}Group: $name")
    buckets.forEach { bucket ->
        val variant = when (bucket) {
            is VariableTypeBucket.Single<*> -> "single"
            is VariableTypeBucket.LightAndDark<*> -> "lightDark"
        }
        appendLine("$prefix  Bucket: ${bucket.name} [$variant]")
        when (bucket) {
            is VariableTypeBucket.Single<*> -> bucket.entries.forEach { appendSingle(prefix, it) }
            is VariableTypeBucket.LightAndDark<*> -> bucket.entries.forEach { appendLightDark(prefix, it) }
        }
    }
    children.forEach { child ->
        append(child.toDebugString(indent + 1))
    }
}

private fun StringBuilder.appendSingle(prefix: String, entry: VariableEntry<*>) {
    appendLine("$prefix    - ${entry.name} = ${entry.value.toDebugString()} (${entry.figmaPath})")
}

private fun StringBuilder.appendLightDark(prefix: String, entry: LightDarkEntry<*>) {
    appendLine("$prefix    - ${entry.name} = light:${entry.light.value.toDebugString()} dark:${entry.dark.value.toDebugString()} (${entry.figmaPath})")
}

private fun VariableValue.toDebugString(): String = when (this) {
    is VariableValue.BooleanValue -> "$value"
    is VariableValue.NumberValue -> "$value"
    is VariableValue.StringValue -> "\"$value\""
    is VariableValue.ColorValue -> value.toHexString()
}
