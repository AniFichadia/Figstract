package com.anifichadia.figstract.ios.importer.asset.model.importing

import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figstract.importer.asset.model.importing.scale
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.Content
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.assetcatalog.Type
import com.anifichadia.figstract.util.FileLockRegistry

/** Note: Make sure the destination is set to [Destination.None], and that the file name doesn't contain any scale suffixes */
fun iosScaleAndStoreInAssetCatalog(
    assetCatalog: AssetCatalog,
    contentName: String,
    type: Type.Image,
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
                    contentName = contentName,
                    type = type,
                    scale = targetScale,
                    fileLockRegistry = fileLockRegistry,
                    idiom = idiom,
                )
        }
        .and()
}

fun iosStoreInAssetCatalog(
    assetCatalog: AssetCatalog,
    contentName: String,
    type: Type.Image,
    scale: Scale,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Idiom = Content.Idiom.default,
) = object : Destination() {
    override suspend fun write(instruction: Instruction, input: ImportPipeline.Output) {
        val outputName = resolveOutputName(instruction, input)
        val extension = resolveExtension(instruction, input)
        assetCatalog
            .contentBuilder(contentName, fileLockRegistry) {
                addImage(
                    name = outputName,
                    extension = extension,
                    content = input.data,
                    type = type,
                    scale = scale,
                    idiom = idiom,
                )
            }
    }
}

fun assetCatalogFinalisationLifecycle(assetCatalog: AssetCatalog): Lifecycle {
    return object : Lifecycle {
        override suspend fun onImportFinished() {
            assetCatalog.finalizeContents()
        }
    }
}
