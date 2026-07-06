package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.cli.core.assets.handler.ArtworkHandlerOptionGroup
import com.anifichadia.figstract.cli.core.assets.handler.IconsHandlerOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.PlatformOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.PlatformOptionGroup.Companion.toPlatformOptions
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import java.io.File

class RealAssetsCommand : BaseAssetsCommand(name = "assets") {
    private val artworkHandlerOptions by ArtworkHandlerOptionGroup()

    private val iconHandlerOptions by IconsHandlerOptionGroup()

    private val platformOptions by PlatformOptionGroup()
    // TODO: iOS Asset catalog namespace

    override fun createHandlers(outDirectory: File): List<AssetFileHandler> {
        val platformOptions = platformOptions.toPlatformOptions()
        if (platformOptions.noneEnabled()) throw BadParameterValue("No platforms have been enabled")

        val artworkConfigs = artworkHandlerOptions.createAssetConfigs(
            outDirectory = outDirectory,
            platformOptions = platformOptions,
        )

        val iconConfigs = iconHandlerOptions.createAssetConfigs(
            outDirectory = outDirectory,
            platformOptions = platformOptions,
        )

        val assetConfigs = listOf(
            artworkConfigs,
            iconConfigs,
        ).flatten()

        return createHandlersFromBatches(assetConfigs, outDirectory)
    }
}
