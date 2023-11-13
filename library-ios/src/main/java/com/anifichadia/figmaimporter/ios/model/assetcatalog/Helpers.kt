package com.anifichadia.figmaimporter.ios.model.assetcatalog

import com.anifichadia.figmaimporter.ios.model.importing.jsonDefault
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

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

fun writeAssetCatalogRootContent(assetRootDirectory: File, json: Json = jsonDefault) {
    File(assetRootDirectory, Content.FILE_NAME).writeText(
        json.encodeToString(
            Content(
                info = Content.Info.xcode,
            )
        )
    )
}
