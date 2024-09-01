package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.AssetCommand
import com.anifichadia.figmaimporter.cli.core.CliHelper
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
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
    private val artworkFilterExcludedCanvases by option("--artworkFilterExcludedCanvas")
        .multiple()
    private val artworkFilterExcludedNodes by option("--artworkFilterExcludedNode")
        .multiple()

    private val iconsEnabled by option("--iconsEnabled")
        .boolean()
        .default(false)
    private val iconsFigmaFile by option("--iconsFigmaFile")
    private val iconFilterExcludedCanvases by option("--iconFilterExcludedCanvas")
        .multiple()
    private val iconFilterExcludedNodes by option("--iconFilterExcludedNode")
        .multiple()

    private val platformAndroid by option("--platformAndroid")
        .boolean()
        .default(false)
    private val platformIos by option("--platformIos")
        .boolean()
        .default(false)
    private val platformWeb by option("--platformWeb")
        .boolean()
        .default(false)

    // This is for testing. Providing a non-null value will run a take operation on the list of all instructions for each handler
    private val instructionLimit: Int? by option("--instructionLimit")
        .int()

    override val createHandlers: CliHelper.HandlerCreator = CliHelper.HandlerCreator { outDirectory ->
        if (!platformAndroid && !platformIos && !platformWeb) error("No platforms have been enabled")

        val androidOutDirectory = File(outDirectory, "android")
        val iosOutDirectory = File(outDirectory, "ios")
        val webOutDirectory = File(outDirectory, "web")

        val artworkFileHandler = if (artworkEnabled) {
            artworkFigmaFile?.let {
                val assetFilter = AssetFilter(
                    excludedCanvases = artworkFilterExcludedCanvases,
                    excludedNodes = artworkFilterExcludedNodes,
                )

                createArtworkFigmaFileHandler(
                    figmaFile = it,
                    createCropped = artworkCreateCropped,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    androidEnabled = platformAndroid,
                    iosEnabled = platformIos,
                    webEnabled = platformWeb,
                    assetFilter = assetFilter,
                    instructionLimit = instructionLimit,
                )
            } ?: error("Artwork is enabled but figma file is not specified")
        } else {
            null
        }

        val iconFileHandler = if (iconsEnabled) {
            iconsFigmaFile?.let {
                val assetFilter = AssetFilter(
                    excludedCanvases = iconFilterExcludedCanvases,
                    excludedNodes = iconFilterExcludedNodes,
                )

                createIconFigmaFileHandler(
                    figmaFile = it,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    androidEnabled = platformAndroid,
                    iosEnabled = platformIos,
                    webEnabled = platformWeb,
                    assetFilter = assetFilter,
                    instructionLimit = instructionLimit,
                )
            } ?: error("Icons are enabled but figma file is not specified")
        } else {
            null
        }

        listOfNotNull(
            artworkFileHandler,
            iconFileHandler,
        )
    }
}
