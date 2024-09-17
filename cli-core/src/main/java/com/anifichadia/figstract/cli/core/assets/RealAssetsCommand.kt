package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.cli.core.assets.handler.ArtworkHandlerOptionGroup
import com.anifichadia.figstract.cli.core.assets.handler.IconsHandlerOptionGroup
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import java.io.File

class RealAssetsCommand : AssetsCommand() {
    private val artworkHandlerOptions by ArtworkHandlerOptionGroup()

    private val iconHandlerOptions by IconsHandlerOptionGroup()

    private val platformOptions by PlatformOptionGroup()

    override fun createHandlers(outDirectory: File): List<AssetFileHandler> {
        if (platformOptions.noneEnabled()) throw BadParameterValue("No platforms have been enabled")

        val androidOutDirectory = File(outDirectory, "android").takeIf { platformOptions.androidEnabled }
        val iosOutDirectory = File(outDirectory, "ios").takeIf { platformOptions.iosEnabled }
        val webOutDirectory = File(outDirectory, "web").takeIf { platformOptions.webEnabled }

        val artworkFileHandler = artworkHandlerOptions.createHandler(
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
        )

        val iconFileHandler = iconHandlerOptions.createHandler(
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
        )

        return listOfNotNull(
            artworkFileHandler,
            iconFileHandler,
        )
    }
}
