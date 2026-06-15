package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.android.importer.variable.model.writer.AndroidComposeVariableDataWriter
import com.anifichadia.figstract.android.importer.variable.model.writer.AndroidXmlVariableDataWriter
import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableFileFilter
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.anifichadia.figstract.importer.variable.model.VariableRenamingMap
import com.anifichadia.figstract.importer.variable.model.writer.JsonVariableDataWriter
import com.anifichadia.figstract.importer.variable.model.writer.VariableDataWriter
import com.anifichadia.figstract.ios.importer.variable.model.writer.IosAssetCatalogVariableDataWriter
import com.anifichadia.figstract.ios.importer.variable.model.writer.IosSwiftUiVariableDataWriter
import com.anifichadia.figstract.type.fold
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.json.Json
import java.io.File

class RealVariablesCommand : VariablesCommand() {
    private val figmaFiles by option("--figmaFile")
        .multiple()
    private val figmaFileBranchName by option("--figmaFileBranchName")
    private val figmaFileVersion by option("--figmaFileVersion")

    private val filters by VariableFilterOptionGroup()
    private val variableOrganizationStrategy by VariableOrganizationStrategyOptionGroup()

    private val outputJson by option("--outputJson")
        .boolean()
        .default(false)
    private val outputAndroidCompose by OutputCodeWithGroupingOptionGroup("AndroidCompose", "PackageName")
    private val outputAndroidXml by AndroidXmlOptionGroup()
    private val outputIosSwiftUi by OutputCodeWithGroupingOptionGroup("IosSwiftUi", "Module")
    private val outputIosAssetCatalog by OutputCodeWithGroupingOptionGroup("IosAssetCatalog", "Namespace")
    private val outputColorAsHex by option("--outputColorAsHex")
        .boolean()
        .default(true)
    private val themeVariantMappings by option("--themeVariantMappingsFile")
        .help(
            """JSON file that contains theme variant mappings (e.g light and dark theming). The file should contain a 
                |JSON object keyed by the variable collection name, and the values are the theme variant mappings""".trimMargin()
        )
        .file(
            mustExist = true,
            canBeFile = true,
            canBeDir = false,
        )
        .convert { file ->
            json.decodeFromString<Map<String, ThemeVariantMapping>>(file.readText())
        }
        .default(emptyMap())

    private val renamingMap by option("--variableRenamingMapFile")
        .help(
            """Path to a JSON file mapping old variable collection and/or variable path names to new names.
            |Collection renames are applied first; variable path renames are then looked up using the resolved (post-rename) collection name. 
            |Format:
            |{
            |   "collections": {
            |       "OldCollectionName": "NewCollectionName"
            |   },
            |   "variables": {
            |       "NewCollectionName": {
            |           "old/variable/path": "new/variable/path"
            |       }
            |   }
            |}""".trimMargin(),
        )
        .file(
            mustExist = true,
            canBeFile = true,
            canBeDir = false,
        )
        .convert { file ->
            try {
                VariableRenamingMap.fromFile(file)
            } catch (e: Exception) {
                throw BadParameterValue("Failed to parse variable renaming map file '$file': ${e.message}")
            }
        }
        .default(VariableRenamingMap.Empty)

    override fun createHandlers(outDirectory: File): List<VariableFileHandler> {
        val filters = filters.toVariableFilter()
        val writers = createWriters(outDirectory, filters)
        if (writers.isEmpty()) throw BadParameterValue("No outputs have been defined")

        return figmaFiles.map { figmaFile ->
            VariableFileHandler(
                figmaFile = figmaFile,
                figmaFileBranchName = figmaFileBranchName,
                figmaFileVersion = figmaFileVersion,
                filter = filters,
                renamingMap = renamingMap,
                themeVariantMappings = themeVariantMappings,
                variableOrganizationStrategy = variableOrganizationStrategy.toVariableOrganizationStrategy(),
                writers = writers,
            )
        }
    }

    @OptIn(ExperimentalFigstractApi::class)
    private fun createWriters(outDirectory: File, filters: VariableFileFilter): List<VariableDataWriter> = buildList {
        if (outputJson) {
            add(
                JsonVariableDataWriter(
                    outDirectory = outDirectory.fold("json"),
                    colorAsHex = outputColorAsHex,
                )
            )
        }
        addIfEnabled(outputAndroidCompose) {
            AndroidComposeVariableDataWriter(
                outDirectory = outDirectory.fold("android", "compose"),
                packageName = it.logicalGrouping,
                colorAsHex = outputColorAsHex,
            )
        }
        addIfEnabled(outputAndroidXml) {
            println("Warning: Android XML variable output is experimental and is subject to change")

            AndroidXmlVariableDataWriter(
                outDirectory = outDirectory.fold("android", "xml"),
                splitByType = it.splitByType,
                namespaceUsingCollectionName = it.namespaceUsingCollectionName,
                numberOutput = it.numberOutput,
            )
        }
        addIfEnabled(outputIosSwiftUi) {
            println("Warning: iOS Swift UI variable output is experimental and is subject to change")

            IosSwiftUiVariableDataWriter(
                outDirectory = outDirectory.fold("ios", "swiftui"),
                modulePath = it.logicalGrouping,
            )
        }
        addIfEnabled(outputIosAssetCatalog) {
            println("Warning: iOS Asset Catalog variable output is experimental and is subject to change")

            if (filters.variableTypeFilter.includeBooleans) {
                println("Warning: iOS Asset Catalog variable output does not support booleans")
            }
            if (filters.variableTypeFilter.includeNumbers) {
                println("Warning: iOS Asset Catalog variable output does not support numbers")
            }

            IosAssetCatalogVariableDataWriter(
                outDirectory = outDirectory.fold("ios", "asset catalog"),
            )
        }
    }

    private fun <T : OutputCodeOptionGroup> MutableList<VariableDataWriter>.addIfEnabled(
        option: T?,
        create: (T) -> VariableDataWriter,
    ) {
        if (option == null || !option.enabled) return

        add(create(option))
    }

    companion object {
        private val json = Json {
            prettyPrint = true
        }
    }
}
