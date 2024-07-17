package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.CliFactory
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

suspend fun main(args: Array<String>) {
    //region Arg management
    val parser = ArgParser("Figma importer")

    //region args
    val artworkEnabled by parser.option(ArgType.Boolean, "artwork.enabled")
        .default(true)
    val artworkFigmaFile by parser.option(ArgType.String, "artwork.figmaFile")
    val artworkCreateCropped by parser.option(ArgType.Boolean, "artwork.createCropped")
        .default(false)

    val iconsEnabled by parser.option(ArgType.Boolean, "icons.enabled")
        .default(true)
    val iconsFigmaFile by parser.option(ArgType.String, "icons.figmaFile")

    val androidEnabled by parser.option(ArgType.Boolean, "platform.android")
        .default(true)
    val iosEnabled by parser.option(ArgType.Boolean, "platform.ios")
        .default(true)
    val webEnabled by parser.option(ArgType.Boolean, "platform.web")
        .default(true)

    // This is for testing. Providing a non-null value will run a take operation on the list of all instructions for each handler
    val instructionLimit: Int? by parser.option(ArgType.Int, "instructionLimit")
    //endregion

    parser.parse(args)
    //endregion

    if (!androidEnabled && !iconsEnabled && !webEnabled) error("No platforms have been enabled")

    CliFactory.createCli(args) { outDirectory ->
        val androidOutDirectory = File(outDirectory, "android")
        val iosOutDirectory = File(outDirectory, "ios")
        val webOutDirectory = File(outDirectory, "web")

        val artworkFileHandler = if (artworkEnabled) {
            artworkFigmaFile?.let {
                createArtworkFigmaFileHandler(
                    figmaFile = it,
                    createCropped = artworkCreateCropped,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    androidEnabled = androidEnabled,
                    iosEnabled = iosEnabled,
                    webEnabled = webEnabled,
                    instructionLimit = instructionLimit,
                )
            } ?: error("Artwork is enabled but figma file is not specified")
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
                    androidEnabled = androidEnabled,
                    iosEnabled = iosEnabled,
                    webEnabled = webEnabled,
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
