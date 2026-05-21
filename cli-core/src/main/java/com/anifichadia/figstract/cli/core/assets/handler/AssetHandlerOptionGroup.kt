package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetFilterOptionGroup
import com.anifichadia.figstract.cli.core.assets.AssetTokenStringGeneratorOptionGroup
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
import com.github.ajalt.clikt.parameters.types.int
import java.io.File

abstract class AssetHandlerOptionGroup(protected val prefix: String) : DelegatableOptionGroup() {
    private val enabled by option("--${prefix}Enabled")
        .boolean()
        .default(false)
    private val figmaFile by option("--${prefix}FigmaFile")
    private val figmaFileBranchName by option("--${prefix}FigmaFileBranchName")
    private val figmaFileVersion by option("--${prefix}FigmaFileVersion")
    private val filters by AssetFilterOptionGroup(prefix)
    private val jsonPath by option("--${prefix}JsonPath")
    private val instructionLimit by option("--${prefix}InstructionLimit").int()
    protected abstract val nameGenerators: AssetTokenStringGeneratorOptionGroup

    fun createHandler(
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
    ): AssetFileHandler? {
        if (enabled) {
            val figmaFile = this.figmaFile
            if (figmaFile != null) {
                return createHandlerInternal(
                    figmaFile = figmaFile,
                    figmaFileBranchName = figmaFileBranchName,
                    figmaFileVersion = figmaFileVersion,
                    androidOutDirectory = androidOutDirectory,
                    iosOutDirectory = iosOutDirectory,
                    webOutDirectory = webOutDirectory,
                    filters = filters,
                    jsonPath = jsonPath,
                    instructionLimit = instructionLimit,
                )
            } else {
                throw BadParameterValue("$prefix are enabled but figma file is not specified")
            }
        }
        return null
    }

    protected abstract fun createHandlerInternal(
        figmaFile: String,
        figmaFileBranchName: String?,
        figmaFileVersion: String?,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
        instructionLimit: Int?,
    ): AssetFileHandler
}

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
        .multiple(default = Scale.entries)
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
        jsonPath: String?,
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
            instructionLimit = instructionLimit,
        )
    }
}

class IconsHandlerOptionGroup : AssetHandlerOptionGroup("icons") {
    override val nameGenerators by AssetTokenStringGeneratorOptionGroup(
        prefix = "icons",
        androidFormat = """ic_{node.name.split "/" last}""",
        iosFormat = """{node.name.split "/" last}""",
        webFormat = """ic_{node.name.split "/" last}""",
    )

    override fun createHandlerInternal(
        figmaFile: String,
        figmaFileBranchName: String?,
        figmaFileVersion: String?,
        androidOutDirectory: File?,
        iosOutDirectory: File?,
        webOutDirectory: File?,
        filters: AssetFilterOptionGroup,
        jsonPath: String?,
        instructionLimit: Int?,
    ): AssetFileHandler {
        return createIconFigmaFileHandler(
            figmaFile = figmaFile,
            figmaFileBranchName = figmaFileBranchName,
            figmaFileVersion = figmaFileVersion,
            androidOutDirectory = androidOutDirectory,
            iosOutDirectory = iosOutDirectory,
            webOutDirectory = webOutDirectory,
            assetFilter = filters.toAssetFilter(),
            androidNameGenerator = nameGenerators.android,
            iosNameGenerator = nameGenerators.ios,
            webNameGenerator = nameGenerators.web,
            jsonPath = jsonPath,
            instructionLimit = instructionLimit,
        )
    }
}
