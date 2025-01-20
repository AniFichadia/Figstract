package com.anifichadia.figstract.ios.importer.variable.model

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.util.FileLockRegistry
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.sanitiseFileName
import java.io.File

@ExperimentalFigstractApi
class IosAssetCatalogVariableDataWriter(
    private val outDirectory: File,
) : VariableDataWriter {
    override suspend fun write(variableData: VariableData) {
        val assetCatalog = AssetCatalog(outDirectory, variableData.variableCollection.name)

        variableData.variablesByMode.forEach { variablesByMode ->
            assetCatalog.contentBuilder(variablesByMode.mode.name, FileLockRegistry()) {
                variablesByMode.colorVariables?.forEach { (name, color) ->
                    addColor(
                        name = name.sanitiseFileName().ToUpperCamelCase(),
                        red = color.r.toFloat(),
                        green = color.g.toFloat(),
                        blue = color.b.toFloat(),
                        alpha = color.a.toFloat(),
                        // TODO: When introducing color scheme mappings, for dark mode, populate this with:
                        //  Appearance(appearance = "luminosity", value = "dark")
                        appearances = null,
                    )
                }
            }
        }

        assetCatalog.finalizeContents()
    }
}
