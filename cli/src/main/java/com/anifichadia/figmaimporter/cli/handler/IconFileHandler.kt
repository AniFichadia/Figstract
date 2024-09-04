package com.anifichadia.figmaimporter.cli.handler

import com.anifichadia.figmaimporter.android.model.importing.androidSvgToAvd
import com.anifichadia.figmaimporter.android.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figmaimporter.cli.AssetFilter
import com.anifichadia.figmaimporter.cli.timingLogger
import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figmaimporter.ios.figma.model.iosIcon
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.model.assetcatalog.createAssetCatalogContentDirectory
import com.anifichadia.figmaimporter.ios.model.assetcatalog.createAssetCatalogRootDirectory
import com.anifichadia.figmaimporter.ios.model.importing.assetCatalogFinalisationLifecycle
import com.anifichadia.figmaimporter.ios.model.importing.iosStoreInAssetCatalog
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.Instruction
import com.anifichadia.figmaimporter.model.Instruction.Companion.addInstruction
import com.anifichadia.figmaimporter.model.exporting.svg
import com.anifichadia.figmaimporter.model.importing.Destination
import com.anifichadia.figmaimporter.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.type.fold
import com.anifichadia.figmaimporter.util.ToUpperCamelCase
import com.anifichadia.figmaimporter.util.sanitise
import com.anifichadia.figmaimporter.util.to_snake_case
import java.io.File

@Suppress("SameParameterValue")
internal fun createIconFigmaFileHandler(
    figmaFile: FileKey,
    androidOutDirectory: File?,
    iosOutDirectory: File?,
    webOutDirectory: File?,
    assetFilter: AssetFilter,
    instructionLimit: Int?,
): FigmaFileHandler {
    val androidImportPipeline = if (androidOutDirectory != null) {
        val androidOutputDirectory = androidOutDirectory.fold("icons", "drawable")
        ImportPipeline(
            steps = androidSvgToAvd then androidVectorColorToPlaceholder,
            destination = Destination.directoryDestination(androidOutputDirectory),
        )
    } else {
        null
    }

    val iosImportPipeline: ImportPipeline?
    val iosAssetCatalogLifecycle: FigmaFileHandler.Lifecycle
    if (iosOutDirectory != null) {
        val iosDirectory = File(iosOutDirectory, "icons")
        val iosAssetCatalogRootDirectory = createAssetCatalogRootDirectory(iosDirectory)
        val iosContentDirectory =
            createAssetCatalogContentDirectory(iosAssetCatalogRootDirectory, "Icon")
        iosImportPipeline = ImportPipeline(
            steps = iosStoreInAssetCatalog(iosContentDirectory, Type.IMAGE_SET, Scale.`1x`),
            // Destination is handled by steps
            destination = Destination.None,
        )
        iosAssetCatalogLifecycle = assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory)
    } else {
        iosImportPipeline = null
        iosAssetCatalogLifecycle = FigmaFileHandler.Lifecycle.NoOp
    }

    val webImportPipeline = if (webOutDirectory != null) {
        val webOutputDirectory = File(webOutDirectory, "icons")
        ImportPipeline(
            destination = Destination.directoryDestination(webOutputDirectory),
        )
    } else {
        null
    }

    val timingLifecycle = FigmaFileHandler.Lifecycle.Timing()
    val timingLoggingLifecycle = object : FigmaFileHandler.Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Icon retrieval timing: \n$timingLifecycle" }
        }
    }

    val iconFileHandler = FigmaFileHandler(
        figmaFile = figmaFile,
        // Icons are smaller, so we can retrieve more at the same time
        assetsPerChunk = 50,
        lifecycle = FigmaFileHandler.Lifecycle.Combined(
            iosAssetCatalogLifecycle,
            timingLifecycle,
            timingLoggingLifecycle,
        ),
    ) { response, _ ->
        val excludedCanvases = assetFilter.excludedCanvases
        val excludedNodes = assetFilter.excludedNodes

        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()
            .filter { it.name.lowercase() !in excludedCanvases }

        canvases.map { canvas ->
            Instruction.buildInstructions {
                canvas.traverseBreadthFirst { node, parent ->
                    if (node.id !in excludedNodes && parent != null && node is Node.Vector) {
                        val parentName = parent.name.let {
                            if (it.contains("/")) {
                                it.split("/")[1]
                            } else {
                                it
                            }
                        }

                        if (androidImportPipeline != null) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = svg,
                                importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                importPipeline = androidImportPipeline,
                            )
                        }

                        if (iosImportPipeline != null) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = iosIcon,
                                importOutputName = parentName.sanitise().ToUpperCamelCase(),
                                importPipeline = iosImportPipeline,
                            )
                        }

                        if (webImportPipeline != null) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = svg,
                                importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                importPipeline = webImportPipeline,
                            )
                        }
                    }
                }
            }
        }.flatten()
            .let {
                if (instructionLimit != null) {
                    it.take(instructionLimit)
                } else {
                    it
                }
            }
    }

    return iconFileHandler
}
