package com.anifichadia.figstract.ios.importer.asset.model.assetcatalog

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Standard [Json] instance used for asset catalogs
 */
val assetCatalogJson = Json {
    prettyPrint = true
}

const val DEFAULT_ASSETS_FILE_NAME = "Assets.xcassets"
val DEFAULT_CONTENT_PROPERTIES = Content.Properties(providesNamespace = true)

fun getAssetCatalogDirectoryName(
    outputName: String,
    type: Type,
    scale: Scale,
    stripScale: Boolean = true,
): String {
    val directoryName = outputName
        .let { directoryName ->
            if (stripScale) {
                scale.removeSuffix(directoryName)
            } else {
                directoryName
            }
        }
        .let { "$it.${type.directorySuffix}" }

    return directoryName
}

fun createAssetCatalogRootDirectory(
    directory: File,
    assetsFileName: String = DEFAULT_ASSETS_FILE_NAME,
): File {
    return File(directory, assetsFileName)
}

fun writeAssetCatalogRootContent(assetCatalogRootDirectory: File) {
    File(assetCatalogRootDirectory, Content.FILE_NAME).writeText(
        assetCatalogJson.encodeToString(
            Content(
                info = Content.Info.xcode,
            )
        )
    )
}

fun createAssetCatalogContentDirectory(
    assetCatalogRootDirectory: File,
    name: String,
): File {
    return File(assetCatalogRootDirectory, name)
}

fun ensureAssetCatalogSubdirectoriesHaveContentFiles(
    assetCatalogRootDirectory: File,
    properties: Content.Properties = DEFAULT_CONTENT_PROPERTIES,
) {
    assetCatalogRootDirectory
        .walkTopDown()
        .filter { it.isDirectory }
        .forEach { subdirectory ->
            val hasContentFile = subdirectory
                .listFiles { file -> file.name == Content.FILE_NAME }
                ?.any()
                ?: false

            if (!hasContentFile) {
                writeAssetCatalogBlankContent(subdirectory, properties)
            }
        }
}

fun writeAssetCatalogBlankContent(
    directory: File,
    properties: Content.Properties = DEFAULT_CONTENT_PROPERTIES,
) {
    File(directory, Content.FILE_NAME).writeText(
        assetCatalogJson.encodeToString(
            Content(
                info = Content.Info.xcode,
                properties = properties,
            )
        )
    )
}
