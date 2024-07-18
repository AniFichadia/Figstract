package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.BooleanChoice
import com.anifichadia.figmaimporter.cli.core.CliFactory
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

suspend fun main(args: Array<String>) {
    //region Arg management
    val parser = CliFactory.createArgParser()

    val artworkEnabled by parser.option(ArgType.BooleanChoice, "artwork.enabled")
        .default(true)
    val artworkFigmaFile by parser.option(ArgType.String, "artwork.figmaFile")
    val artworkCreateCropped by parser.option(ArgType.BooleanChoice, "artwork.createCropped")
        .default(false)

    val iconsEnabled by parser.option(ArgType.BooleanChoice, "icons.enabled")
        .default(true)
    val iconsFigmaFile by parser.option(ArgType.String, "icons.figmaFile")

    val androidEnabled by parser.option(ArgType.BooleanChoice, "platform.android")
        .default(true)
    val iosEnabled by parser.option(ArgType.BooleanChoice, "platform.ios")
        .default(true)
    val webEnabled by parser.option(ArgType.BooleanChoice, "platform.web")
        .default(true)

    // This is for testing. Providing a non-null value will run a take operation on the list of all instructions for each handler
    val instructionLimit: Int? by parser.option(ArgType.Int, "instructionLimit")
    //endregion

    CliFactory.createCli(args, parser = parser) { outDirectory ->
        if (!androidEnabled && !iconsEnabled && !webEnabled) error("No platforms have been enabled")

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
