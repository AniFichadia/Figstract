package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
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
        .flag(default = false)
    override val namers by AssetTokenStringGeneratorOptionGroup(
        prefix = prefix,
        androidFormat = "artwork_{canvas.name}_{parentNode.name}",
        iosFormat = "artwork_{canvas.name}_{parentNode.name}",
        webFormat = "artwork_{canvas.name}_{parentNode.name}",
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
            jsonPath = jsonPath,
        )
    }
}

class IconsHandlerOptionGroup : AssetHandlerOptionGroup("icons") {
    override val namers by AssetTokenStringGeneratorOptionGroup(
        prefix = "icons",
        androidFormat = "ic_{parentNode.splitName}",
        iosFormat = "{parentNode.splitName}",
        webFormat = "ic_{parentNode.splitName}",
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
            jsonPath = jsonPath,
        )
    }
}
