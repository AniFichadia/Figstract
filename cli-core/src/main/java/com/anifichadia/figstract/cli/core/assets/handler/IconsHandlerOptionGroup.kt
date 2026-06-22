package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.assets.model.AssetRenamingMap
import com.anifichadia.figstract.cli.core.assets.model.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.assets.option.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import java.io.File

class IconsHandlerOptionGroup : AssetHandlerOptionGroup("icons") {
    override val nameGenerators by AssetTokenStringGeneratorOptionGroup(
        prefix = "icons",
        androidFormat = """ic_{node.name}""",
        iosFormat = """{node.name}""",
        webFormat = """{node.name}""",
    )

    override fun createHandlerInternal(
        figmaFileDefinition: FigmaFileDefinition,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        renamingMap: AssetRenamingMap,
        jsonPath: String?,
        iosGroupByToken: NodeTokenStringGenerator?,
        instructionLimit: Int?,
    ): AssetFileHandler {
        return createIconFigmaFileHandler(
            figmaFileDefinition = figmaFileDefinition,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            assetFilter = filters.toAssetFilter(),
            renamingMap = renamingMap,
            androidNameGenerator = nameGenerators.android,
            iosNameGenerator = nameGenerators.ios,
            webNameGenerator = nameGenerators.web,
            jsonPath = jsonPath,
            iosGroupByToken = iosGroupByToken,
            instructionLimit = instructionLimit,
        )
    }
}
