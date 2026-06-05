package com.anifichadia.figstract.android.importer.variable.model.writer

import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variabletree.LightDarkEntry
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableEntry
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableTypeBucket
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableValue
import com.anifichadia.figstract.importer.variable.model.writer.VariableDataWriter
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.sanitise
import com.anifichadia.figstract.util.sanitiseFileName
import com.anifichadia.figstract.util.to_snake_case
import java.io.File

/**
 * Android XML resource writer. Flattens the [VariableGroup] tree into resource names
 * using `_` as a separator between path segments.
 *
 * [VariableTypeBucket.LightAndDark] buckets write light values to `values/` and dark
 * values to `values-night/`. [VariableTypeBucket.Single] buckets write to `values/` only.
 *
 * When [splitByType] is true, each variable type is written to a separate file.
 * When false, all types are merged into a single file per qualifier directory.
 */
// TODO: do not prefix entries based on collection name
class AndroidXmlVariableDataWriter(
    private val outDirectory: File,
    private val splitByType: Boolean = true,
    private val numberOutput: NumberOutput = NumberOutput.INTEGER,
) : VariableDataWriter {
    init {
        require(!outDirectory.exists() || outDirectory.isDirectory) {
            "outDirectory must be a directory: $outDirectory"
        }
    }

    override suspend fun write(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
        collectionName: String,
        root: VariableGroup,
    ) {
        val collectionResourceName = collectionName.sanitiseFileName().to_snake_case()
        val hasLightDark = root.hasLightDarkEntries()

        val resDirectory = outDirectory.fold("res", "values")
        val resNightDirectory = outDirectory.fold("res", "values-night")

        if (hasLightDark) {
            writeResourceFiles(
                fileBaseName = collectionResourceName,
                dir = resDirectory,
                root = root,
                isLight = true,
            )

            writeResourceFiles(
                fileBaseName = collectionResourceName,
                dir = resNightDirectory,
                root = root,
                isLight = false,
            )
        } else {
            writeResourceFiles(
                fileBaseName = collectionResourceName,
                dir = resDirectory,
                root = root,
                isLight = null,
            )
        }
    }

    private fun VariableGroup.hasLightDarkEntries(): Boolean {
        return buckets.any {
            it is VariableTypeBucket.LightAndDark.Booleans ||
                it is VariableTypeBucket.LightAndDark.Numbers ||
                it is VariableTypeBucket.LightAndDark.Strings ||
                it is VariableTypeBucket.LightAndDark.Colors
        } || children.any { it.hasLightDarkEntries() }
    }

    private fun writeResourceFiles(
        fileBaseName: String,
        dir: File,
        root: VariableGroup,
        isLight: Boolean?,
    ) {
        if (splitByType) {
            writeBooleanFile(fileBaseName, dir, root, isLight)
            writeNumberFile(fileBaseName, dir, root, isLight)
            writeStringFile(fileBaseName, dir, root, isLight)
            writeColorFile(fileBaseName, dir, root, isLight)
        } else {
            writeXmlFile(dir, fileBaseName) {
                appendBooleanResources(root, null, isLight)
                appendIntegerResources(root, null, isLight)
                appendStringResources(root, null, isLight)
                appendColorResources(root, null, isLight)
            }
        }
    }

    private fun writeBooleanFile(
        fileBaseName: String,
        dir: File,
        root: VariableGroup,
        isLight: Boolean?,
    ) {
        writeXmlFile(dir, "${fileBaseName}_booleans") {
            appendBooleanResources(root, prefix = null, isLight = isLight)
        }
    }

    private fun writeNumberFile(
        fileBaseName: String,
        dir: File,
        root: VariableGroup,
        isLight: Boolean?,
    ) {
        if (numberOutput == NumberOutput.NONE) return

        val suffix = when (numberOutput) {
            NumberOutput.INTEGER -> "integers"
            NumberOutput.DIMEN -> "dimens"
            NumberOutput.FLOAT -> "floats"
        }

        writeXmlFile(dir, "${fileBaseName}_$suffix") {
            when (numberOutput) {
                NumberOutput.INTEGER -> appendIntegerResources(root, null, isLight)
                NumberOutput.DIMEN -> appendDimenResources(root, null, isLight)
                NumberOutput.FLOAT -> appendFloatResources(root, null, isLight)
            }
        }
    }

    private fun writeStringFile(
        fileBaseName: String,
        dir: File,
        root: VariableGroup,
        isLight: Boolean?,
    ) {
        writeXmlFile(dir, "${fileBaseName}_strings") {
            appendStringResources(root, prefix = null, isLight = isLight)
        }
    }

    private fun writeColorFile(
        fileBaseName: String,
        dir: File,
        root: VariableGroup,
        isLight: Boolean?,
    ) {
        writeXmlFile(dir, "${fileBaseName}_colors") {
            appendColorResources(root, prefix = null, isLight = isLight)
        }
    }

    private fun StringBuilder.appendBooleanResources(
        group: VariableGroup,
        prefix: String?,
        isLight: Boolean?,
    ) {
        val tagName = "bool"
        val nextPrefix = groupPrefix(group, prefix)

        group.buckets.forEach { bucket ->
            when (bucket) {
                is VariableTypeBucket.Single.Booleans -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.value.value,
                        )
                    }
                }

                is VariableTypeBucket.LightAndDark.Booleans -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.resolve(isLight).value.value,
                        )
                    }
                }

                else -> Unit
            }
        }

        group.children.forEach {
            appendBooleanResources(it, nextPrefix, isLight)
        }
    }

    private fun StringBuilder.appendIntegerResources(
        group: VariableGroup,
        prefix: String?,
        isLight: Boolean?,
    ) {
        val tagName = "integer"
        val nextPrefix = groupPrefix(group, prefix)

        group.buckets.forEach { bucket ->
            when (bucket) {
                is VariableTypeBucket.Single.Numbers -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.value.value,
                        )
                    }
                }

                is VariableTypeBucket.LightAndDark.Numbers -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.resolve(isLight).value.value,
                        )
                    }
                }

                else -> Unit
            }
        }

        group.children.forEach {
            appendIntegerResources(it, nextPrefix, isLight)
        }
    }

    private fun StringBuilder.appendDimenResources(
        group: VariableGroup,
        prefix: String?,
        isLight: Boolean?,
    ) {
        val tagName = "dimen"
        val nextPrefix = groupPrefix(group, prefix)

        group.buckets.forEach { bucket ->
            when (bucket) {
                is VariableTypeBucket.Single.Numbers -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = "${entry.value.value}dp",
                        )
                    }
                }

                is VariableTypeBucket.LightAndDark.Numbers -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = "${entry.resolve(isLight).value.value}dp",
                        )
                    }
                }

                else -> Unit
            }
        }

        group.children.forEach {
            appendDimenResources(it, nextPrefix, isLight)
        }
    }

    private fun StringBuilder.appendFloatResources(
        group: VariableGroup,
        prefix: String?,
        isLight: Boolean?,
    ) {
        val tagName = "item"
        val otherAttributes = mapOf(
            "type" to "dimen",
            "format" to "float",
        )
        val nextPrefix = groupPrefix(group, prefix)

        group.buckets.forEach { bucket ->
            when (bucket) {
                is VariableTypeBucket.Single.Numbers -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.value.value,
                            otherAttributes = otherAttributes,
                        )
                    }
                }

                is VariableTypeBucket.LightAndDark.Numbers -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = tagName,
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.resolve(isLight).value.value,
                            otherAttributes = otherAttributes,
                        )
                    }
                }

                else -> Unit
            }
        }

        group.children.forEach {
            appendFloatResources(it, nextPrefix, isLight)
        }
    }

    private fun StringBuilder.appendStringResources(
        group: VariableGroup,
        prefix: String?,
        isLight: Boolean?,
    ) {
        val nextPrefix = groupPrefix(group, prefix)

        group.buckets.forEach { bucket ->
            when (bucket) {
                is VariableTypeBucket.Single.Strings -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = "string",
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.value.value.escapeXml(),
                        )
                    }
                }

                is VariableTypeBucket.LightAndDark.Strings -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = "string",
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.resolve(isLight).value.value.escapeXml(),
                        )
                    }
                }

                else -> Unit
            }
        }

        group.children.forEach {
            appendStringResources(it, nextPrefix, isLight)
        }
    }

    private fun StringBuilder.appendColorResources(
        group: VariableGroup,
        prefix: String?,
        isLight: Boolean?,
    ) {
        val nextPrefix = groupPrefix(group, prefix)

        group.buckets.forEach { bucket ->
            when (bucket) {
                is VariableTypeBucket.Single.Colors -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = "color",
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.value.value.toHexString(),
                        )
                    }
                }

                is VariableTypeBucket.LightAndDark.Colors -> {
                    bucket.entries.forEach { entry ->
                        appendXmlEntry(
                            tagName = "color",
                            name = resourceName(entry.name, nextPrefix),
                            value = entry.resolve(isLight).value.value.toHexString(),
                        )
                    }
                }

                else -> Unit
            }
        }

        group.children.forEach {
            appendColorResources(it, nextPrefix, isLight)
        }
    }

    private fun groupPrefix(
        group: VariableGroup,
        prefix: String?,
    ): String {
        val groupName = group.name.sanitise().ToUpperCamelCase().replaceFirstChar { it.lowercase() }

        return if (prefix != null) {
            "${prefix}_${groupName}"
        } else {
            groupName
        }
    }

    private fun resourceName(
        name: String,
        prefix: String?,
    ): String {
        val leafName = name.sanitiseResourceName()

        return if (prefix != null) {
            "${prefix}_${leafName}"
        } else {
            leafName
        }
    }

    private fun <V : VariableValue> LightDarkEntry<V>.resolve(
        isLight: Boolean?,
    ): VariableEntry<V> {
        return if (isLight != false) {
            light
        } else {
            dark
        }
    }

    private fun writeXmlFile(
        dir: File,
        baseName: String,
        block: StringBuilder.() -> Unit,
    ) {
        val content = buildString(block)
        if (content.isBlank()) return

        dir.mkdirs()

        dir.fold("$baseName.xml").writeText(
            buildString {
                appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
                appendLine("<resources>")
                append(content)
                appendLine("</resources>")
            }
        )
    }

    private fun StringBuilder.appendXmlEntry(
        tagName: String,
        name: String,
        value: Any,
        otherAttributes: Map<String, String> = emptyMap(),
        indent: String = "    ",
    ) {
        val attributes = (mapOf("name" to name) + otherAttributes)
            .entries
            .joinToString(" ") { (name, value) ->
                """$name="$value""""
            }

        appendLine(
            """$indent<$tagName $attributes>$value</$tagName>""",
        )
    }

    private fun String.sanitiseResourceName() = this
        .sanitise()
        .to_snake_case(lowercase = false)
        .replaceFirstChar { it.lowercase() }

    private fun String.escapeXml(): String = this
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("'", "\\'")
        .replace("\"", "\\\"")

    enum class NumberOutput {
        NONE,

        /** Emits `<integer>`. Fractional values are truncated. */
        INTEGER,

        /** Emits `<dimen>` with a `dp` unit suffix. */
        DIMEN,

        /** Emits `<item type="dimen" format="float">` (no unit). */
        FLOAT,
    }
}
