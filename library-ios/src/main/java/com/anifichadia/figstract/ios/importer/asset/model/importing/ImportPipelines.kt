package com.anifichadia.figstract.ios.importer.asset.model.importing

import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.Destination.Companion.directoryDestination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figstract.importer.asset.model.importing.renameSuffix
import com.anifichadia.figstract.importer.asset.model.importing.scale
import com.anifichadia.figstract.ios.assetcatalog.Content
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.assetcatalog.Type
import com.anifichadia.figstract.ios.assetcatalog.asFileSuffix
import com.anifichadia.figstract.ios.assetcatalog.ensureAssetCatalogSubdirectoriesHaveContentFiles
import com.anifichadia.figstract.ios.assetcatalog.writeAssetCatalogRootContent
import com.anifichadia.figstract.util.FileLockRegistry
import java.io.File

/** Note: Make sure the destination is set to [Destination.None], and that the file name doesn't contain any scale suffixes */
fun iosScaleAndStoreInAssetCatalog(
    contentDirectory: File,
    type: Type,
    sourceScale: Scale,
    scales: List<Scale> = Scale.entries,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Idiom = Content.Idiom.default,
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
    idiom: Content.Idiom = Content.Idiom.default,
): ImportPipeline.Step {
    return renameSuffix(scale.asFileSuffix()) then
            appendAssetDirectoryPathElements(type, scale, true) then
            updateAssetCatalog(outputDirectory, fileLockRegistry, scale, idiom) then
            directoryDestination(outputDirectory)
}

fun assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory: File): Lifecycle {
    return object : Lifecycle {
        override suspend fun onImportFinished() {
            iosAssetCatalogRootDirectory.mkdirs()
            writeAssetCatalogRootContent(iosAssetCatalogRootDirectory)
            ensureAssetCatalogSubdirectoriesHaveContentFiles(iosAssetCatalogRootDirectory)
        }
    }
}
