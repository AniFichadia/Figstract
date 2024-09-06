package com.anifichadia.figmaimporter.ios.importer.asset.model.importing

import com.anifichadia.figmaimporter.importer.asset.model.AssetFileHandler
import com.anifichadia.figmaimporter.importer.asset.model.importing.Destination
import com.anifichadia.figmaimporter.importer.asset.model.importing.Destination.Companion.directoryDestination
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.importer.asset.model.importing.renameSuffix
import com.anifichadia.figmaimporter.importer.asset.model.importing.scale
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.Content
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.asFileSuffix
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.ensureAssetCatalogSubdirectoriesHaveContentFiles
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.writeAssetCatalogRootContent
import com.anifichadia.figmaimporter.util.FileLockRegistry
import java.io.File

/** Note: Make sure the destination is set to [Destination.None], and that the file name doesn't contain any scale suffixes */
fun iosScaleAndStoreInAssetCatalog(
    contentDirectory: File,
    type: Type,
    sourceScale: Scale,
    scales: List<Scale> = Scale.entries,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Image.Idiom = Content.Image.Idiom.default,
): ImportPipeline.Step {
    return scales
        .map { targetScale ->
            scale(sourceScale.scaleRelativeTo(targetScale)) then
                iosStoreInAssetCatalog(contentDirectory, type, targetScale, fileLockRegistry, idiom)
        }
        .and()
}

fun iosStoreInAssetCatalog(
    outputDirectory: File,
    type: Type,
    scale: Scale,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Image.Idiom = Content.Image.Idiom.default,
): ImportPipeline.Step {
    return renameSuffix(scale.asFileSuffix()) then
            appendAssetDirectoryPathElements(type, scale, true) then
            updateAssetCatalog(outputDirectory, fileLockRegistry, scale, idiom) then
            directoryDestination(outputDirectory)
}

fun assetCatalogFinalisationLifecycle(iosIconAssetCatalogRootDirectory: File): AssetFileHandler.Lifecycle {
    return object : AssetFileHandler.Lifecycle {
        override suspend fun onImportFinished() {
            iosIconAssetCatalogRootDirectory.mkdirs()
            writeAssetCatalogRootContent(iosIconAssetCatalogRootDirectory)
            ensureAssetCatalogSubdirectoriesHaveContentFiles(iosIconAssetCatalogRootDirectory)
        }
    }
}
