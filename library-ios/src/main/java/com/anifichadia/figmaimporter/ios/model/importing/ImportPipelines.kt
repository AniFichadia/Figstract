package com.anifichadia.figmaimporter.ios.model.importing

import com.anifichadia.figmaimporter.ios.model.assetcatalog.Content
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.model.assetcatalog.asFileSuffix
import com.anifichadia.figmaimporter.ios.model.assetcatalog.writeAssetCatalogRootContent
import com.anifichadia.figmaimporter.model.importing.Destination
import com.anifichadia.figmaimporter.model.importing.Destination.Companion.directoryDestination
import com.anifichadia.figmaimporter.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.and
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.passthrough
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.model.importing.renameSuffix
import com.anifichadia.figmaimporter.model.importing.scale
import com.anifichadia.figmaimporter.util.FileLockRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Note: Make sure the destination is set to [Destination.None], and that the file name doesn't contain any scale suffixes */
fun ios3xDownscaleAndStoreInImageAssetCatalog(
    directory: File,
    assetsFileName: String = "Assets.xcassets",
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    properties: Content.Properties = Content.Properties(providesNamespace = true),
    idiom: Content.Image.Idiom = Content.Image.Idiom.default,
    json: Json = jsonDefault,
): ImportPipeline.Step {
    val assetRootDirectory = File(directory, assetsFileName)
    assetRootDirectory.mkdirs()
    writeAssetCatalogRootContent(assetRootDirectory, json)
    val outputDirectory = assetCatalogImagesDirectory(assetRootDirectory, properties, json)

    val `1xPipeline` = scale(1 / 3f) then
            iosImageScale(outputDirectory, fileLockRegistry, idiom, json, Scale.`1x`)
    val `2xPipeline` = scale(2 / 3f) then
            iosImageScale(outputDirectory, fileLockRegistry, idiom, json, Scale.`2x`)
    val `3xPipeline` = passthrough() then
            iosImageScale(outputDirectory, fileLockRegistry, idiom, json, Scale.`3x`)

    return listOf(`1xPipeline`, `2xPipeline`, `3xPipeline`).and()
}

fun iosImageScale(
    outputDirectory: File,
    fileLockRegistry: FileLockRegistry = FileLockRegistry(),
    idiom: Content.Image.Idiom = Content.Image.Idiom.default,
    json: Json = jsonDefault,
    scale: Scale,
): ImportPipeline.Step {
    return renameSuffix(scale.asFileSuffix()) then
            assetCatalogPath(Type.IMAGE_SET, scale, true) then
            updateImagesetAssetCatalog(outputDirectory, fileLockRegistry, scale, true, idiom, json) then
            directoryDestination(outputDirectory)
}

fun assetCatalogImagesDirectory(
    assetRootDirectory: File,
    properties: Content.Properties = Content.Properties(providesNamespace = true),
    json: Json = jsonDefault,
): File {
    val imagesDirectory = File(assetRootDirectory, "Images")
    imagesDirectory.mkdirs()

    File(imagesDirectory, Content.FILE_NAME).writeText(
        json.encodeToString(
            Content(
                info = Content.Info.xcode,
                properties = properties,
            )
        )
    )

    return imagesDirectory
}
