package com.anifichadia.figstract.ios.importer.asset.model.importing

import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.passThrough
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figstract.importer.asset.model.importing.convertToPngLossy
import com.anifichadia.figstract.importer.asset.model.importing.scale
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.AssetType
import com.anifichadia.figstract.ios.assetcatalog.Content
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.util.FileLockRegistry

/** Note: Make sure the destination is set to [Destination.None], and that the file name doesn't contain any scale suffixes */
fun iosScaleAndStoreInAssetCatalog(
    assetCatalog: AssetCatalog,
    assetType: AssetType.Image,
    sourceScale: Scale,
    scales: List<Scale> = Scale.defaults,
    outputFormat: ArtworkOutputFormat = ArtworkOutputFormat.Default,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Idiom = Content.Idiom.default,
    groupByPathElements: Boolean = false,
): ImportPipeline.Step {
    return scales
        .map { targetScale ->
            scale(sourceScale.scaleRelativeTo(targetScale)) then
                when (outputFormat) {
                    ArtworkOutputFormat.Heic -> convertToHeic()
                    ArtworkOutputFormat.PngLossy -> convertToPngLossy()
                    ArtworkOutputFormat.Default -> passThrough()
                } then
                iosStoreInAssetCatalog(
                    assetCatalog = assetCatalog,
                    assetType = assetType,
                    scale = targetScale,
                    fileLockRegistry = fileLockRegistry,
                    idiom = idiom,
                    groupByPathElements = groupByPathElements,
                )
        }
        .and()
}

fun iosStoreInAssetCatalog(
    assetCatalog: AssetCatalog,
    assetType: AssetType.Image,
    scale: Scale,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Idiom = Content.Idiom.default,
    groupByPathElements: Boolean = false,
) = object : Destination() {
    override suspend fun write(instruction: Instruction, input: ImportPipeline.Output) {
        val outputName = resolveOutputName(instruction, input)
        val extension = resolveExtension(instruction, input)
        val groups = buildList {
            if (groupByPathElements) addAll(instruction.import.importTarget.pathElements)
            add(AssetCatalog.GroupName.Images.directoryName)
        }
        assetCatalog.contentBuilder(
            groups = groups,
            fileLockRegistry = fileLockRegistry,
        ) {
            addImage(
                name = outputName,
                extension = extension,
                content = input.data,
                assetType = assetType,
                scale = scale,
                idiom = idiom,
            )
        }
    }
}
