package com.anifichadia.figstract.ios.importer.variable.model.writer

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variabletree.LightDarkEntry
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableEntry
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableTypeBucket
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableValue
import com.anifichadia.figstract.importer.variable.model.writer.VariableDataWriter
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.COLOR_APPEARANCE_DARK_MODE
import com.anifichadia.figstract.util.FileLockRegistry
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.sanitise
import com.anifichadia.figstract.util.sanitiseFileName
import java.io.File

@ExperimentalFigstractApi
class IosAssetCatalogVariableDataWriter(
    private val outDirectory: File,
) : VariableDataWriter {
    override suspend fun write(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
        collectionName: String,
        root: VariableGroup,
    ) {
        val assetCatalog = AssetCatalog(outDirectory, collectionName)
        val fileLockRegistry = FileLockRegistry()

        writeGroup(
            group = root,
            assetCatalog = assetCatalog,
            fileLockRegistry = fileLockRegistry,
            parentGroups = listOf(AssetCatalog.GroupName.Colors.directoryName),
        )
    }

    private suspend fun writeGroup(
        group: VariableGroup,
        assetCatalog: AssetCatalog,
        fileLockRegistry: FileLockRegistry,
        parentGroups: List<String>,
    ) {
        val colorBuckets = group.buckets.filter {
            it is VariableTypeBucket.Single.Colors || it is VariableTypeBucket.LightAndDark.Colors
        }

        if (colorBuckets.isNotEmpty()) {
            assetCatalog.contentBuilder(groups = parentGroups, fileLockRegistry = fileLockRegistry) {
                colorBuckets.forEach { bucket ->
                    when (bucket) {
                        is VariableTypeBucket.Single.Colors ->
                            bucket.entries.forEach { addSingleColor(it) }

                        is VariableTypeBucket.LightAndDark.Colors ->
                            bucket.entries.forEach { addLightDarkColor(it) }

                        else -> Unit
                    }
                }
            }
        }

        group.children.forEach { child ->
            writeGroup(
                group = child,
                assetCatalog = assetCatalog,
                fileLockRegistry = fileLockRegistry,
                parentGroups = parentGroups + child.name.sanitise(),
            )
        }
    }

    private suspend fun AssetCatalog.ContentBuilder.addSingleColor(entry: VariableEntry<VariableValue.ColorValue>) {
        val name = entry.name.sanitiseFileName().ToUpperCamelCase()
        val color = entry.value.value
        addColor(
            name = name,
            red = color.r.toFloat(),
            green = color.g.toFloat(),
            blue = color.b.toFloat(),
            alpha = color.a.toFloat(),
            appearances = null,
        )
    }

    private suspend fun AssetCatalog.ContentBuilder.addLightDarkColor(entry: LightDarkEntry<VariableValue.ColorValue>) {
        val name = entry.name.sanitiseFileName().ToUpperCamelCase()
        val light = entry.light.value.value
        val dark = entry.dark.value.value
        addColor(
            name = name,
            red = light.r.toFloat(),
            green = light.g.toFloat(),
            blue = light.b.toFloat(),
            alpha = light.a.toFloat(),
            appearances = null,
        )
        addColor(
            name = name,
            red = dark.r.toFloat(),
            green = dark.g.toFloat(),
            blue = dark.b.toFloat(),
            alpha = dark.a.toFloat(),
            appearances = COLOR_APPEARANCE_DARK_MODE,
        )
    }
}
