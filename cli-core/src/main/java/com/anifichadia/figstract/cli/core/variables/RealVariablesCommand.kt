package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.android.importer.variable.model.AndroidComposeVariableDataWriter
import com.anifichadia.figstract.importer.variable.model.JsonVariableDataWriter
import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.anifichadia.figstract.ios.importer.variable.model.IosAssetCatalogVariableDataWriter
import com.anifichadia.figstract.ios.importer.variable.model.IosSwiftUiVariableDataWriter
import com.anifichadia.figstract.type.fold
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
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

    private val filters by VariableFilterOptionGroup()

    private val outputJson by option("--outputJson")
        .boolean()
        .default(false)
    private val outputAndroidCompose by OutputCodeOptionGroup("AndroidCompose", "PackageName")
    private val outputIosSwiftUi by OutputCodeOptionGroup("IosSwiftUi", "Module")
    private val outputIosAssetCatalog by OutputCodeOptionGroup("IosAssetCatalog", "Namespace")
    private val outputColorAsHex by option("--outputColorAsHex")
        .boolean()
        .default(true)
    private val themeVariantMappingsFile by option("--themeVariantMappingsFile")
        .help(
            """JSON file that contains theme variant mappings (e.g light and dark theming). The file should contain a 
                |JSON object keyed by the variable collection name, and the values are the theme variant mappings""".trimMargin()
        )
        .file(
            mustExist = true,
            canBeFile = true,
            canBeDir = false,
        )

    override fun createHandlers(outDirectory: File): List<VariableFileHandler> {
        val themeVariantMappingFile = themeVariantMappingsFile
        val themeVariantMappings = if (themeVariantMappingFile != null) {
            json.decodeFromString<Map<String, ThemeVariantMapping>>(themeVariantMappingFile.readText())
        } else {
            emptyMap()
        }

        val writers = createWriters(outDirectory)
        if (writers.isEmpty()) throw BadParameterValue("No outputs have been defined")

        return figmaFiles.map { figmaFile ->
            VariableFileHandler(
                figmaFile = figmaFile,
                filter = filters.toVariableFilter(),
                themeVariantMappings = themeVariantMappings,
                writers = writers,
            )
        }
    }

    @OptIn(ExperimentalFigstractApi::class)
    private fun createWriters(outDirectory: File): List<VariableDataWriter> = buildList {
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
        addIfEnabled(outputIosSwiftUi) {
            println("Warning: iOS Swift UI variable output is experimental and is subject to change")

            IosSwiftUiVariableDataWriter(
                outDirectory = outDirectory.fold("ios", "swiftui"),
                modulePath = it.logicalGrouping,
            )
        }
        addIfEnabled(outputIosAssetCatalog) {
            println("Warning: iOS Asset Catalog variable output is experimental and is subject to change")

            IosAssetCatalogVariableDataWriter(
                outDirectory = outDirectory.fold("ios", "asset catalog"),
            )
        }
    }

    private fun MutableList<VariableDataWriter>.addIfEnabled(
        option: OutputCodeOptionGroup?,
        create: (OutputCodeOptionGroup) -> VariableDataWriter,
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
