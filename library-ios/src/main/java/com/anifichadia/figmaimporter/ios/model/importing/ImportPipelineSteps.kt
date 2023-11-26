package com.anifichadia.figmaimporter.ios.model.importing

import com.anifichadia.figmaimporter.ios.model.assetcatalog.Content
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.model.assetcatalog.assetCatalogJson
import com.anifichadia.figmaimporter.ios.model.assetcatalog.getAssetCatalogDirectoryName
import com.anifichadia.figmaimporter.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolvePathElements
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.sideEffect
import com.anifichadia.figmaimporter.type.fold
import com.anifichadia.figmaimporter.type.replaceOrAdd
import com.anifichadia.figmaimporter.util.FileLockRegistry
import kotlinx.serialization.encodeToString
import java.io.File

// TODO: add heif conversion: https://github.com/gotson/NightMonkeys/tree/main/imageio-heif

fun appendAssetDirectoryPathElements(
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

fun updateAssetCatalog(
    directory: File,
    fileLockRegistry: FileLockRegistry,
    scale: Scale,
    idiom: Content.Image.Idiom = Content.Image.Idiom.default,
): ImportPipeline.Step {
    return sideEffect { instruction, input ->
        val outputName = resolveOutputName(instruction, input)
        val outputPathElements = resolvePathElements(instruction, input)
        val contentDirectory = directory.fold(outputPathElements)
        contentDirectory.mkdirs()
        val contentsFile = File(contentDirectory, Content.FILE_NAME)
        val extension = resolveExtension(instruction, input)

        fileLockRegistry.withLock(contentsFile) {
            val contentToUpdate = if (contentsFile.exists()) {
                assetCatalogJson.decodeFromString<Content>(contentsFile.readText())
            } else {
                Content(info = Content.Info.xcode)
            }
            val updatedContent = contentToUpdate.copy(
                images = (contentToUpdate.images ?: emptyList())
                    .replaceOrAdd(
                        predicate = { it.scale == scale },
                        replacement = {
                            Content.Image(
                                idiom = idiom,
                                scale = scale,
                                filename = "$outputName.$extension",
                            )
                        },
                    )
                    // Ensures file is deterministically generated
                    .sortedBy { it.scale }
            )

            contentsFile.writeText(assetCatalogJson.encodeToString(updatedContent))
        }
    }
}
