package com.anifichadia.figmaimporter.ios.model.importing

import com.anifichadia.figmaimporter.ios.model.assetcatalog.Content
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.model.assetcatalog.getAssetCatalogDirectoryName
import com.anifichadia.figmaimporter.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.sideEffect
import com.anifichadia.figmaimporter.type.replaceOrAdd
import com.anifichadia.figmaimporter.util.FileLockRegistry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

// TODO: add heif conversion: https://github.com/gotson/NightMonkeys/tree/main/imageio-heif

fun assetCatalogPath(
    type: Type,
    scale: Scale,
    stripScale: Boolean = true,
): ImportPipeline.Step {
    return ImportPipeline.Step("assetCatalogPath(type: $type, scale: $scale, stripScale: $stripScale)") { instruction, input ->
        val outputName = resolveOutputName(instruction, input)
        val directoryName = getAssetCatalogDirectoryName(outputName, type, scale, stripScale)

        input
            .copy(
                target = input.target.copy(
                    pathElements = (input.target.pathElements + listOf(directoryName)),
                )
            )
            .single()
    }
}

fun updateImagesetAssetCatalog(
    directory: File,
    fileLockRegistry: FileLockRegistry,
    scale: Scale,
    stripScale: Boolean = true,
    idiom: Content.Image.Idiom = Content.Image.Idiom.default,
    json: Json = jsonDefault,
): ImportPipeline.Step {
    return sideEffect { instruction, input ->
        val outputName = resolveOutputName(instruction, input)
        val directoryName = getAssetCatalogDirectoryName(outputName, Type.IMAGE_SET, scale, stripScale)
        val contentDirectory = File(directory, directoryName)
        contentDirectory.mkdirs()
        val contentsFile = File(contentDirectory, Content.FILE_NAME)
        val extension = resolveExtension(instruction, input)

        fileLockRegistry.withLock(contentsFile) {
            val contentToUpdate = if (contentsFile.exists()) {
                json.decodeFromString<Content>(contentsFile.readText())
            } else {
                Content(info = Content.Info.xcode)
            }
            val updatedContent = contentToUpdate.copy(
                images = (contentToUpdate.images ?: emptyList()).replaceOrAdd(
                    predicate = { it.scale == scale },
                    replacement = {
                        Content.Image(
                            idiom = idiom,
                            scale = scale,
                            filename = "$outputName.$extension",
                        )
                    },
                )
            )

            contentsFile.writeText(json.encodeToString(updatedContent))
        }
    }
}

val jsonDefault = Json {
    prettyPrint = true
}
