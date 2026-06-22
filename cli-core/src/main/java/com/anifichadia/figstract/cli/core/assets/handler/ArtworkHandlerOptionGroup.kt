package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.cli.core.assets.option.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.option.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import com.anifichadia.figstract.importer.asset.model.NodeTokenStringGenerator
import com.anifichadia.figstract.importer.asset.model.exporting.pngUnscaled
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.figma.model.ios3xImage
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
        .multiple(default = DensityBucket.defaults)
        .unique()
    private val artworkIosOutputScales by option("--${prefix}IosOutputScales")
        .enum<Scale>()
        .multiple(default = Scale.defaults)
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

    override fun createHandlerInternal(
        figmaFileDefinition: FigmaFileDefinition,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        renamingMap: AssetRenamingMap,
        jsonPath: String?,
        iosGroupByToken: NodeTokenStringGenerator?,
        instructionLimit: Int?,
    ): AssetFileHandler {
        if (!(artworkCreateUncropped || artworkCreateCropped)) throw BadParameterValue("Atleast createUncropped or createCropped must be set to true")

        return createArtworkFigmaFileHandler(
            figmaFileDefinition = figmaFileDefinition,
            createUncropped = artworkCreateUncropped,
            createCropped = artworkCreateCropped,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            assetFilter = filters.toAssetFilter(),
            renamingMap = renamingMap,
            androidNameGenerator = nameGenerators.android,
            iosNameGenerator = nameGenerators.ios,
            webNameGenerator = nameGenerators.web,
            jsonPath = jsonPath,
            androidExportConfig = androidImageXxxHdpi,
            iosExportConfig = ios3xImage,
            webExportConfig = pngUnscaled,
            androidOutputDensityBuckets = artworkAndroidOutputDensityBuckets.toList(),
            iosOutputScales = artworkIosOutputScales.toList(),
            iosOutputFormat = artworkIosOutputFormat,
            iosGroupByToken = iosGroupByToken,
            instructionLimit = instructionLimit,
        )
    }
}
