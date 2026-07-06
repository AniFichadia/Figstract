package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.assets.model.AssetConfig
import com.anifichadia.figstract.cli.core.assets.model.NamingFormats
import com.anifichadia.figstract.cli.core.assets.model.PlatformOptions
import com.anifichadia.figstract.cli.core.assets.option.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.asset.model.AssetFilter
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import java.io.File

class IconsHandlerOptionGroup : AssetHandlerOptionGroup("icons") {
    override val nameGenerators by AssetTokenStringGeneratorOptionGroup(
        prefix = "icons",
        androidFormat = """ic_{node.name}""",
        iosFormat = """{node.name}""",
        webFormat = """{node.name}""",
    )

    override fun createAssetConfigsInternal(
        figmaFileDefinition: FigmaFileDefinition,
        outDirectory: File,
        platformOptions: PlatformOptions,
        assetFilter: AssetFilter,
        renamingMap: AssetRenamingMap,
        jsonPath: String?,
        iosGroupByTokenNamingFormat: String?,
        instructionLimit: Int?,
    ): List<AssetConfig> {
        return listOf(
            AssetConfig.Icon(
                fileDefinition = figmaFileDefinition,
                enabled = true,
                outDirectory = null, // Relies on default out dir resolution
                assetFilter = assetFilter,
                renamingMap = renamingMap,
                namingFormats = NamingFormats(
                    androidFormat = nameGenerators.android.format,
                    iosFormat = nameGenerators.ios.format,
                    webFormat = nameGenerators.web.format,
                    webCasing = nameGenerators.web.casing,
                ),
                jsonPath = jsonPath,
                platformOptions = platformOptions,
                iosGroupByTokenNamingFormat = iosGroupByTokenNamingFormat,
                instructionLimit = instructionLimit
            )
        )
    }
}
