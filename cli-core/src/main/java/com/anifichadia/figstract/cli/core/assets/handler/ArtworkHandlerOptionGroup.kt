package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.cli.core.assets.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetRenamingMap
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup
import com.anifichadia.figstract.cli.core.assets.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.provideDelegate
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.exporting.pngUnscaled
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.figma.model.ios3xImage
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
    private val artworkIosConvertToHeic by option("--${prefix}IosConvertToHeic")
        .boolean()
        .default(false)
    override val nameGenerators by AssetTokenStringGeneratorOptionGroup(
        prefix = prefix,
        androidFormat = "artwork_{canvas.name}_{node.name}",
        iosFormat = "Artwork{canvas.name}{node.name}",
        webFormat = "artwork_{canvas.name}_{node.name}",
    )

    override fun createHandlerInternal(
        figmaFile: String,
        figmaFileBranchName: String?,
        figmaFileVersion: String?,
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
            figmaFile = figmaFile,
            figmaFileBranchName = figmaFileBranchName,
            figmaFileVersion = figmaFileVersion,
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
            iosConvertToHeic = artworkIosConvertToHeic,
            iosGroupByToken = iosGroupByToken,
            instructionLimit = instructionLimit,
        )
    }
}
