package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.AssetCommand
import com.anifichadia.figmaimporter.cli.handler.createArtworkFigmaFileHandler
import com.anifichadia.figmaimporter.cli.handler.createIconFigmaFileHandler
import com.anifichadia.figmaimporter.importer.asset.model.AssetFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.int
import java.io.File

class RealAssetCommand : AssetCommand() {
    private val artworkEnabled by option("--artworkEnabled")
        .boolean()
        .default(false)
    private val artworkFigmaFile by option("--artworkFigmaFile")
    private val artworkCreateCropped by option("--artworkCreateCropped")
        .flag(default = false)
    private val artworkFilter by AssetFilterOptionGroup("artwork")

    private val iconsEnabled by option("--iconsEnabled")
        .boolean()
        .default(false)
    private val iconsFigmaFile by option("--iconsFigmaFile")
    private val iconFilter by AssetFilterOptionGroup("icon")

    private val platformOptions by PlatformOptionGroup()

    // This is for testing. Providing a non-null value will run a take operation on the list of all instructions for each handler
    private val instructionLimit: Int? by option("--instructionLimit")
        .int()

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
                    instructionLimit = instructionLimit,
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
                    instructionLimit = instructionLimit,
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
