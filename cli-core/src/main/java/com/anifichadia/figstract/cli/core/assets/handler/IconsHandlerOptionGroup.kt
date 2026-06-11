package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.assets.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.assets.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import java.io.File

class IconsHandlerOptionGroup : AssetHandlerOptionGroup("icons") {
    override val nameGenerators by AssetTokenStringGeneratorOptionGroup(
        prefix = "icons",
        androidFormat = """ic_{node.name.split "/" last}""",
        iosFormat = """{node.name.split "/" last}""",
        webFormat = """ic_{node.name.split "/" last}""",
    )

    override fun createHandlerInternal(
        figmaFile: String,
        figmaFileBranchName: String?,
        figmaFileVersion: String?,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
        iosGroupByToken: NodeTokenStringGenerator?,
        instructionLimit: Int?,
    ): AssetFileHandler {
        return createIconFigmaFileHandler(
            figmaFile = figmaFile,
            figmaFileBranchName = figmaFileBranchName,
            figmaFileVersion = figmaFileVersion,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            assetFilter = filters.toAssetFilter(),
            androidNameGenerator = nameGenerators.android,
            iosNameGenerator = nameGenerators.ios,
            webNameGenerator = nameGenerators.web,
            jsonPath = jsonPath,
            iosGroupByToken = iosGroupByToken,
            instructionLimit = instructionLimit,
        )
    }
}
