package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.cli.core.assets.handler.createArtworkFigmaFileHandler
import com.anifichadia.figstract.cli.core.assets.handler.createIconFigmaFileHandler
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import java.io.File

class RealAssetsCommand : AssetsCommand() {
    private val artworkEnabled by option("--artworkEnabled")
        .boolean()
        .default(false)
    private val artworkFigmaFile by option("--artworkFigmaFile")
    private val artworkCreateCropped by option("--artworkCreateCropped")
        .flag(default = false)
    private val artworkFilter by AssetFilterOptionGroup("artwork")
    private val artworkJsonPath by option("--artworkJsonPath")

    private val iconsEnabled by option("--iconsEnabled")
        .boolean()
        .default(false)
    private val iconsFigmaFile by option("--iconsFigmaFile")
    private val iconFilter by AssetFilterOptionGroup("icons")
    private val iconsJsonPath by option("--iconsJsonPath")

    private val platformOptions by PlatformOptionGroup()

    override fun createHandlers(outDirectory: File): List<AssetFileHandler> {
        if (platformOptions.noneEnabled()) throw BadParameterValue("No platforms have been enabled")

        val androidOutDirectory = File(outDirectory, "android").takeIf { platformOptions.androidEnabled }
        val iosOutDirectory = File(outDirectory, "ios").takeIf { platformOptions.iosEnabled }
        val webOutDirectory = File(outDirectory, "web").takeIf { platformOptions.webEnabled }

        val artworkFileHandler = if (artworkEnabled) {
            artworkFigmaFile?.let {
                createArtworkFigmaFileHandler(
                    figmaFile = it,
                    createCropped = artworkCreateCropped,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    assetFilter = artworkFilter.toAssetFilter(),
                    jsonPath = artworkJsonPath,
                )
            } ?: throw BadParameterValue("Artwork is enabled but figma file is not specified")
        } else {
            null
        }

        val iconFileHandler = if (iconsEnabled) {
            iconsFigmaFile?.let {
                createIconFigmaFileHandler(
                    figmaFile = it,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    assetFilter = iconFilter.toAssetFilter(),
                    jsonPath = iconsJsonPath,
                )
            } ?: throw BadParameterValue("Icons are enabled but figma file is not specified")
        } else {
            null
        }

        return listOfNotNull(
            artworkFileHandler,
            iconFileHandler,
        )
    }
}
