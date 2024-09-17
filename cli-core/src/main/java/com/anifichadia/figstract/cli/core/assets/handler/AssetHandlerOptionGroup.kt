package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import java.io.File

abstract class AssetHandlerOptionGroup(protected val prefix: String) : DelegatableOptionGroup() {
    private val enabled by option("--${prefix}Enabled")
        .boolean()
        .default(false)
    private val figmaFile by option("--${prefix}FigmaFile")
    private val filters by AssetFilterOptionGroup(prefix)
    private val jsonPath by option("--${prefix}JsonPath")
    protected abstract val namers: AssetTokenStringGeneratorOptionGroup

    fun createHandler(
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
    ): AssetFileHandler? {
        if (enabled) {
            figmaFile?.let {
                return createHandlerInternal(
                    figmaFile = it,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    filters = filters,
                    jsonPath = jsonPath,
                )
            } ?: throw BadParameterValue("$prefix are enabled but figma file is not specified")
        }
        return null
    }

    protected abstract fun createHandlerInternal(
        figmaFile: String,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
    ): AssetFileHandler
}

class ArtworkHandlerOptionGroup : AssetHandlerOptionGroup("artwork") {
    private val artworkCreateCropped by option("--${prefix}CreateCropped")
        .boolean()
        .default(false)
    override val namers by AssetTokenStringGeneratorOptionGroup(
        prefix = prefix,
        androidFormat = "artwork_{canvas.name}_{node.name}",
        iosFormat = "artwork_{canvas.name}_{node.name}",
        webFormat = "artwork_{canvas.name}_{node.name}",
    )

    override fun createHandlerInternal(
        figmaFile: String,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
    ): AssetFileHandler {
        return createArtworkFigmaFileHandler(
            figmaFile = figmaFile,
            createCropped = artworkCreateCropped,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            assetFilter = filters.toAssetFilter(),
            androidNamer = namers.android,
            iosNamer = namers.ios,
            webNamer = namers.web,
            jsonPath = jsonPath,
        )
    }
}

class IconsHandlerOptionGroup : AssetHandlerOptionGroup("icons") {
    override val namers by AssetTokenStringGeneratorOptionGroup(
        prefix = "icons",
        androidFormat = """ic_{node.name.split "/" last}""",
        iosFormat = """{node.name.split "/" last}""",
        webFormat = """ic_{node.name.split "/" last}""",
    )

    override fun createHandlerInternal(
        figmaFile: String,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
    ): AssetFileHandler {
        return createIconFigmaFileHandler(
            figmaFile = figmaFile,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            assetFilter = filters.toAssetFilter(),
            androidNamer = namers.android,
            iosNamer = namers.ios,
            webNamer = namers.web,
            jsonPath = jsonPath,
        )
    }
}
