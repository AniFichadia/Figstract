package com.anifichadia.figstract.ios.importer.asset.model.importing

import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Output.Companion.single
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveExtension
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.resolvePathElements
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.sideEffect
import com.anifichadia.figstract.ios.assetcatalog.Content
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.assetcatalog.Type
import com.anifichadia.figstract.ios.assetcatalog.assetCatalogJson
import com.anifichadia.figstract.ios.assetcatalog.getAssetCatalogDirectoryName
import com.anifichadia.figstract.type.fold
import com.anifichadia.figstract.type.replaceOrAdd
import com.anifichadia.figstract.util.FileLockRegistry
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
