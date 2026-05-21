package com.anifichadia.figstract.android.importer.variable.model

import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.importer.variable.model.ResolvedThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.type.noOp
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.sanitise
import com.anifichadia.figstract.util.sanitiseFileName
import com.anifichadia.figstract.util.to_snake_case
import java.io.File

/**
 * Android XML resource writer. This will provide different output structures based on [splitByType] and [numberOutput]:
 * - res/values/bools_<collection>_<mode>.xml
 * - res/values/integers_<collection>_<mode>.xml
 * - res/values/strings_<collection>_<mode>.xml
 * - res/values/colors_<collection>_<mode>.xml
 * - res/values/dimens_<collection>_<mode>.xml  (if numberOutput == DIMEN)
 * - res/values/floats_<collection>_<mode>.xml  (if numberOutput == FLOAT)
 *
 * [ResolvedThemeVariantMapping.LightAndDark] will use values and values-night directories respectively
 *
 * When [splitByType] is false, all types are merged into a single file per qualifier directory.
 */
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
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    ) {
        val collectionName = variableData.variableCollection.name.sanitiseFileName().to_snake_case()

        when (resolvedThemeVariantMapping) {
            is ResolvedThemeVariantMapping.LightAndDark -> {
                writeLightDarkFiles(collectionName, resolvedThemeVariantMapping)
            }

            is ResolvedThemeVariantMapping.None -> {
                variableData.variablesByMode.forEach { modeData ->
                    writeModeFiles(collectionName, modeData)
                }
            }
        }
    }

    //region LightAndDark
    private fun writeLightDarkFiles(
        collectionName: String,
        mapping: ResolvedThemeVariantMapping.LightAndDark,
    ) {
        val lightDir = outDirectory.fold("res", "values")
        val darkDir = outDirectory.fold("res", "values-night")

        writeResourceFiles(
            dir = lightDir,
            fileBaseName = collectionName,
            booleans = mapping.booleans.mapValues { it.value.resolve(isLight = true) },
            numbers = mapping.numbers.mapValues { it.value.resolve(isLight = true) },
            strings = mapping.strings.mapValues { it.value.resolve(isLight = true) },
            colors = mapping.colors.mapValues { it.value.resolve(isLight = true) },
        )

        writeResourceFiles(
            dir = darkDir,
            fileBaseName = collectionName,
            booleans = mapping.booleans.mapValues { it.value.resolve(isLight = false) },
            numbers = mapping.numbers.mapValues { it.value.resolve(isLight = false) },
            strings = mapping.strings.mapValues { it.value.resolve(isLight = false) },
            colors = mapping.colors.mapValues { it.value.resolve(isLight = false) },
        )
    }
    //endregion

    //region None
    private fun writeModeFiles(
        collectionName: String,
        modeData: VariableData.VariablesByMode,
    ) {
        val dir = outDirectory.fold("res", "values")
        val modeSuffix = modeData.mode.name.sanitiseFileName().to_snake_case()

        writeResourceFiles(
            dir = dir,
            fileBaseName = "${collectionName}_${modeSuffix}",
            booleans = modeData.booleanVariables ?: emptyMap(),
            numbers = modeData.numberVariables ?: emptyMap(),
            strings = modeData.stringVariables ?: emptyMap(),
            colors = modeData.colorVariables ?: emptyMap(),
        )
    }
    //endregion

    //region XML generation
    /**
     * Dispatches to either split-by-type files or a single merged file depending on [splitByType].
     */
    private fun writeResourceFiles(
        dir: File,
        fileBaseName: String,
        booleans: Map<String, Boolean>,
        numbers: Map<String, Double>,
        strings: Map<String, String>,
        colors: Map<String, Color>,
    ) {
        if (splitByType) {
            if (booleans.isNotEmpty()) {
                writeXmlFile(dir, "${fileBaseName}_booleans") { appendBooleans(booleans) }
            }
            if (numbers.isNotEmpty()) {
                when (numberOutput) {
                    NumberOutput.NONE -> noOp()
                    NumberOutput.INTEGER -> writeXmlFile(dir, "${fileBaseName}_integers") {
                        appendIntegers(numbers)
                    }

                    NumberOutput.DIMEN -> writeXmlFile(dir, "${fileBaseName}_dimens") {
                        appendDimens(numbers)
                    }

                    NumberOutput.FLOAT -> writeXmlFile(dir, "${fileBaseName}_floats") {
                        appendFloats(numbers)
                    }
                }
            }
            if (strings.isNotEmpty()) {
                writeXmlFile(dir, "${fileBaseName}_strings") {
                    appendStrings(strings)
                }
            }
            if (colors.isNotEmpty()) {
                writeXmlFile(dir, "${fileBaseName}_colors") {
                    appendColors(colors)
                }
            }
        } else {
            val sections: List<StringBuilder.() -> Unit> = buildList {
                if (booleans.isNotEmpty()) add { appendBooleans(booleans) }
                if (numbers.isNotEmpty()) {
                    when (numberOutput) {
                        NumberOutput.NONE -> noOp()
                        NumberOutput.INTEGER -> add {
                            appendIntegers(numbers)
                        }

                        NumberOutput.DIMEN -> add {
                            appendDimens(numbers)
                        }

                        NumberOutput.FLOAT -> add {
                            appendFloats(numbers)
                        }
                    }
                }
                if (strings.isNotEmpty()) add {
                    appendStrings(strings)
                }
                if (colors.isNotEmpty()) add {
                    appendColors(colors)
                }
            }

            if (sections.isEmpty()) return

            writeXmlFile(dir, fileBaseName) {
                sections.forEachIndexed { index, section ->
                    if (index > 0) appendLine()
                    section()
                }
            }
        }
    }

    //region XML writing and generation
    private fun writeXmlFile(dir: File, baseName: String, block: StringBuilder.() -> Unit) {
        writeXmlFile(dir, baseName, buildXmlFile(block))
    }

    private fun writeXmlFile(dir: File, baseName: String, xml: String) {
        dir.mkdirs()
        dir.fold("$baseName.xml").writeText(xml)
    }

    private fun buildXmlFile(block: StringBuilder.() -> Unit) = buildString {
        appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
        appendLine("<resources>")
        block()
        appendLine("</resources>")
    }

    private fun StringBuilder.appendBooleans(booleans: Map<String, Boolean>) {
        if (booleans.isEmpty()) return
        booleans.forEach { (name, value) ->
            appendLine("""    <bool name="${name.toResourceName()}">$value</bool>""")
        }
    }

    private fun StringBuilder.appendIntegers(numbers: Map<String, Double>) {
        if (numbers.isEmpty()) return
        numbers.forEach { (name, value) ->
            appendLine("""    <integer name="${name.toResourceName()}">${value.toLong()}</integer>""")
        }
    }

    private fun StringBuilder.appendDimens(numbers: Map<String, Double>) {
        if (numbers.isEmpty()) return
        numbers.forEach { (name, value) ->
            appendLine("""    <dimen name="${name.toResourceName()}">${value}dp</dimen>""")
        }
    }

    private fun StringBuilder.appendFloats(numbers: Map<String, Double>) {
        if (numbers.isEmpty()) return
        numbers.forEach { (name, value) ->
            appendLine("""    <item name="${name.toResourceName()}" type="dimen" format="float">$value</item>""")
        }
    }

    private fun StringBuilder.appendStrings(strings: Map<String, String>) {
        if (strings.isEmpty()) return
        strings.forEach { (name, value) ->
            appendLine("""    <string name="${name.toResourceName()}">${value.escapeXml()}</string>""")
        }
    }

    private fun StringBuilder.appendColors(colors: Map<String, Color>) {
        if (colors.isEmpty()) return
        colors.forEach { (name, color) ->
            appendLine("""    <color name="${name.toResourceName()}">#${color.toHexString()}</color>""")
        }
    }
    //endregion

    /**
     * Converts a Figma variable name to a valid Android resource name:
     * lowercase, spaces and hyphens replaced by underscores, slashes replaced by double-underscores.
     */
    private fun String.toResourceName(): String {
        return this
            .sanitise()
            .replace("/", "_")
            .ToUpperCamelCase()
    }

    private fun String.escapeXml(): String {
        return this
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
    }
    //endregion

    enum class NumberOutput {
        NONE,

        /** Emits `<integer>`. Fractional values are truncated */
        INTEGER,

        /** Emits `<dimen>` with a `dp` unit suffix */
        DIMEN,

        /** Emits `<item type="dimen" format="float">` (no unit) */
        FLOAT,
    }
}
