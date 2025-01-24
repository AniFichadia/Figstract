package com.anifichadia.figstract.ios.importer.variable.model

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.importer.variable.model.ResolvedThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.COLOR_APPEARANCE_DARK_MODE
import com.anifichadia.figstract.util.FileLockRegistry
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.sanitiseFileName
import java.io.File

@ExperimentalFigstractApi
class IosAssetCatalogVariableDataWriter(
    private val outDirectory: File,
) : VariableDataWriter {
    override suspend fun write(
        variableData: VariableData,
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    ) {
        val assetCatalog = AssetCatalog(outDirectory, variableData.variableCollection.name)
        val fileLockRegistry = FileLockRegistry()

        when (resolvedThemeVariantMapping) {
            is ResolvedThemeVariantMapping.LightAndDark -> {
                assetCatalog.contentBuilder(
                    groups = listOf(AssetCatalog.GroupName.Colors.directoryName),
                    fileLockRegistry = fileLockRegistry,
                ) {
                    resolvedThemeVariantMapping.colors.forEach { (name, colors) ->
                        val colorsAndAppearances = when (colors) {
                            is ResolvedThemeVariantMapping.LightAndDark.Value.LightOnly -> listOf(
                                colors.light to null,
                            )

                            is ResolvedThemeVariantMapping.LightAndDark.Value.DarkOnly -> listOf(
                                colors.dark to COLOR_APPEARANCE_DARK_MODE,
                            )

                            is ResolvedThemeVariantMapping.LightAndDark.Value.Both -> listOf(
                                colors.light to null,
                                colors.dark to COLOR_APPEARANCE_DARK_MODE,
                            )
                        }
                        colorsAndAppearances.forEach { (color, appearances) ->
                            addColor(
                                name = name.sanitiseFileName().ToUpperCamelCase(),
                                red = color.r.toFloat(),
                                green = color.g.toFloat(),
                                blue = color.b.toFloat(),
                                alpha = color.a.toFloat(),
                                appearances = appearances,
                            )
                        }
                    }
                }
            }

            is ResolvedThemeVariantMapping.None -> {
                variableData.variablesByMode.forEach { variablesByMode ->
                    assetCatalog.contentBuilder(
                        groups = listOf(AssetCatalog.GroupName.Colors.directoryName, variablesByMode.mode.name),
                        fileLockRegistry = fileLockRegistry,
                    ) {
                        variablesByMode.colorVariables?.forEach { (name, color) ->
                            addColor(
                                name = name.sanitiseFileName().ToUpperCamelCase(),
                                red = color.r.toFloat(),
                                green = color.g.toFloat(),
                                blue = color.b.toFloat(),
                                alpha = color.a.toFloat(),
                                appearances = null,
                            )
                        }
                    }
                }
            }
        }
    }
}
