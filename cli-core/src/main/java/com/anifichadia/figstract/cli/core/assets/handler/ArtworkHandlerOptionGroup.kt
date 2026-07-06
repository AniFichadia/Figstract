package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.cli.core.assets.model.AssetConfig
import com.anifichadia.figstract.cli.core.assets.model.NamingFormats
import com.anifichadia.figstract.cli.core.assets.model.PlatformOptions
import com.anifichadia.figstract.cli.core.assets.option.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.asset.model.AssetFilter
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.importer.asset.model.importing.ArtworkOutputFormat
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.unique
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import java.io.File

class ArtworkHandlerOptionGroup : AssetHandlerOptionGroup("artwork") {
    private val artworkCreateUncropped by option("--${prefix}CreateUncropped")
        .boolean()
        .default(true)
    private val artworkCreateCropped by option("--${prefix}CreateCropped")
        .boolean()
        .default(false)
    private val artworkAndroidOutputDensityBuckets by option("--${prefix}AndroidOutputDensityBuckets")
        .enum<DensityBucket>()
        .multiple(default = DensityBucket.defaults.toList())
        .unique()
    private val artworkIosOutputScales by option("--${prefix}IosOutputScales")
        .enum<Scale>()
        .multiple(default = Scale.defaults.toList())
        .unique()
    private val artworkIosOutputFormat by option("--${prefix}IosOutputFormat")
        .enum<ArtworkOutputFormat>()
        .default(ArtworkOutputFormat.Default)
    override val nameGenerators by AssetTokenStringGeneratorOptionGroup(
        prefix = prefix,
        androidFormat = """{canvas.name}_{node.name}""",
        iosFormat = """{canvas.name}{node.name}""",
        webFormat = """{canvas.name}_{node.name}""",
    )

    override fun createAssetConfigsInternal(
        figmaFileDefinition: FigmaFileDefinition,
        outDirectory: File,
        platformOptions: PlatformOptions,
        assetFilter: AssetFilter,
        renamingMap: AssetRenamingMap,
        jsonPath: String?,
        iosGroupByTokenNamingFormat: String?,
        instructionLimit: Int?,
    ): List<AssetConfig> {
        if (!(artworkCreateUncropped || artworkCreateCropped)) throw BadParameterValue("Atleast createUncropped or createCropped must be set to true")

        return listOf(
            AssetConfig.Artwork(
                fileDefinition = figmaFileDefinition,
                enabled = true,
                outDirectory = null, // Relies on default out dir resolution
                assetFilter = assetFilter,
                renamingMap = renamingMap,
                namingFormats = NamingFormats(
                    androidFormat = nameGenerators.android.format,
                    iosFormat = nameGenerators.ios.format,
                    webFormat = nameGenerators.web.format,
                    webCasing = nameGenerators.web.casing,
                ),
                jsonPath = jsonPath,
                platformOptions = platformOptions,
                iosGroupByTokenNamingFormat = iosGroupByTokenNamingFormat,
                instructionLimit = instructionLimit,
                createUncropped = artworkCreateUncropped,
                createCropped = artworkCreateCropped,
                androidOutputDensityBuckets = artworkAndroidOutputDensityBuckets,
                iosOutputScales = artworkIosOutputScales,
                iosOutputFormat = artworkIosOutputFormat,
            )
        )
    }
}
