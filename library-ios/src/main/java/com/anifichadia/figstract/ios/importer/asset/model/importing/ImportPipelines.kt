package com.anifichadia.figstract.ios.importer.asset.model.importing

import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then
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
    scales: List<Scale> = Scale.entries,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Idiom = Content.Idiom.default,
): ImportPipeline.Step {
    return scales
        .map { targetScale ->
            scale(sourceScale.scaleRelativeTo(targetScale)) then
                iosStoreInAssetCatalog(
                    assetCatalog = assetCatalog,
                    assetType = assetType,
                    scale = targetScale,
                    fileLockRegistry = fileLockRegistry,
                    idiom = idiom,
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
) = object : Destination() {
    override suspend fun write(instruction: Instruction, input: ImportPipeline.Output) {
        val outputName = resolveOutputName(instruction, input)
        val extension = resolveExtension(instruction, input)
        assetCatalog.contentBuilder(
            groups = listOf(AssetCatalog.GroupName.Images.directoryName),
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
