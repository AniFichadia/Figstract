package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.CliFactory
import java.io.File

suspend fun main(args: Array<String>) {
    val androidEnabled = true
    val iosEnabled = true
    val webEnabled = true

    val artworkEnabled = true
    val artworkCreateCropped = true
    val iconsEnabled = true

    // This is for testing. Providing a non-null value will run a take operation on the list of all instructions for each handler
    @Suppress("RedundantNullableReturnType")
    val instructionLimit: Int? = null

    CliFactory.createCli(args) { outDirectory ->
        val androidOutDirectory = File(outDirectory, "android")
        val iosOutDirectory = File(outDirectory, "ios")
        val webOutDirectory = File(outDirectory, "web")

        val artworkFileHandler = createArtworkFigmaFileHandler(
            enabled = artworkEnabled,
            createCropped = artworkCreateCropped,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            androidEnabled = androidEnabled,
            iosEnabled = iosEnabled,
            webEnabled = webEnabled,
            instructionLimit = instructionLimit,
        )

        val iconFileHandler = createIconFigmaFileHandler(
            enabled = iconsEnabled,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            androidEnabled = androidEnabled,
            iosEnabled = iosEnabled,
            webEnabled = webEnabled,
            instructionLimit = instructionLimit,
        )

        listOfNotNull(
            artworkFileHandler,
            iconFileHandler,
        )
    }
}
