package com.anifichadia.figstract.ios.importer.asset.model.importing.dsl

import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineDslException
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineStepRegistry.Companion.buildImportPipelineStepRegistry
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.AssetType
import com.anifichadia.figstract.ios.assetcatalog.Content
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.importer.asset.model.importing.ArtworkOutputFormat
import com.anifichadia.figstract.ios.importer.asset.model.importing.HEIC_LOSSY_QUALITY_PERCENT_DEFAULT
import com.anifichadia.figstract.ios.importer.asset.model.importing.convertToHeic
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosScaleAndStoreInAssetCatalog
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosStoreInAssetCatalog
import com.anifichadia.figstract.util.FileLockRegistry
import java.io.File

val IosImportPipelineStepRegistry = buildImportPipelineStepRegistry {
    "convertToHeic" withFactory { params ->
        val qualityPercent = params.valueOrDefault<Int>("qualityPercent") { HEIC_LOSSY_QUALITY_PERCENT_DEFAULT }
        convertToHeic(qualityPercent)
    }
}

/**
 * @param fileLockRegistry Shared across all steps so concurrent catalog writes are serialised correctly.
 */
fun iosAssetCatalogStepRegistry(
    baseDirectory: File,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
): ImportPipelineStepRegistry {
    return buildImportPipelineStepRegistry {
        "iosStoreInAssetCatalog" withFactory { params ->
            val assetCatalog = params.resolveAssetCatalog(baseDirectory)
            val scale = params.resolveScale("scale")
            val assetType = params.resolveAssetType()
            val idiom = params.resolveIdiom()

            iosStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                assetType = assetType,
                scale = scale,
                fileLockRegistry = fileLockRegistry,
                idiom = idiom,
            )
        }

        "iosScaleAndStoreInAssetCatalog" withFactory { params ->
            val assetCatalog = params.resolveAssetCatalog(baseDirectory)
            val sourceScale = params.resolveScale("sourceScale")
            val assetType = params.resolveAssetType()
            val scales = params.resolveScales()
            val idiom = params.resolveIdiom()
            val outputFormat = params.resolveOutputFormat()

            iosScaleAndStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                assetType = assetType,
                sourceScale = sourceScale,
                scales = scales,
                outputFormat = outputFormat,
                fileLockRegistry = fileLockRegistry,
                idiom = idiom,
            )
        }
    }
}

//region Param helpers
private fun ImportPipelineStepRegistry.StepParams.resolveAssetCatalog(baseDirectory: File): AssetCatalog {
    val path = value<String>("path")
    val catalogName = valueOrDefault("catalogName", default = { AssetCatalog.DEFAULT_ASSETS_FILE_NAME })
    return AssetCatalog(
        parentDirectory = baseDirectory.resolve(path),
        assetsFileName = catalogName,
    )
}

private fun ImportPipelineStepRegistry.StepParams.resolveScale(key: String): Scale = enum<Scale>(key)

private fun ImportPipelineStepRegistry.StepParams.resolveScales(): List<Scale> {
    return enumListOrDefault("scales") { Scale.defaults }
}

private fun ImportPipelineStepRegistry.StepParams.resolveAssetType(): AssetType.Image =
    when (valueOrDefault("assetType") { "imageset" }) {
        "imageset" -> AssetType.Image.ImageSet
        "iconset" -> AssetType.Image.IconSet
        else -> throw ImportPipelineDslException(
            "Parameter 'assetType' must be 'imageset' or 'iconset', got '${this["assetType"]}'"
        )
    }

private fun ImportPipelineStepRegistry.StepParams.resolveIdiom(): Content.Idiom =
    enumOrDefault("idiom") { Content.Idiom.default }

private fun ImportPipelineStepRegistry.StepParams.resolveOutputFormat(): ArtworkOutputFormat =
    enumOrDefault("outputFormat") { ArtworkOutputFormat.Default }
//endregion
