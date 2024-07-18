package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.android.model.importing.androidSvgToAvd
import com.anifichadia.figmaimporter.android.model.importing.androidVectorColorToPlaceholder
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
    androidOutDirectory: File,
    iosOutDirectory: File,
    webOutDirectory: File,
    androidEnabled: Boolean,
    iosEnabled: Boolean,
    webEnabled: Boolean,
    instructionLimit: Int?,
): FigmaFileHandler {
    val androidOutputDirectory = androidOutDirectory.fold("icons", "drawable")
    val androidImportPipeline = ImportPipeline(
        steps = androidSvgToAvd then androidVectorColorToPlaceholder,
        destination = Destination.directoryDestination(androidOutputDirectory),
    )

    val iosDirectory = File(iosOutDirectory, "icons")
    val iosAssetCatalogRootDirectory = createAssetCatalogRootDirectory(iosDirectory)
    val iosContentDirectory =
        createAssetCatalogContentDirectory(iosAssetCatalogRootDirectory, "Icon")
    val iosImportPipeline = ImportPipeline(
        steps = iosStoreInAssetCatalog(iosContentDirectory, Type.IMAGE_SET, Scale.`1x`),
        // Destination is handled by steps
        destination = Destination.None,
    )
    val iosAssetCatalogLifecycle = if (iosEnabled) {
        assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory)
    } else {
        FigmaFileHandler.Lifecycle.NoOp
    }

    val webOutputDirectory = File(webOutDirectory, "icons")
    val webImportPipeline = ImportPipeline(
        destination = Destination.directoryDestination(webOutputDirectory),
    )

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
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()

        canvases.map { canvas ->
            Instruction.buildInstructions {
                canvas.traverseBreadthFirst { node, parent ->
                    if (parent != null && node is Node.Vector) {
                        val parentName = parent.name.let {
                            if (it.contains("/")) {
                                it.split("/")[1]
                            } else {
                                it
                            }
                            }

                        if (androidEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = svg,
                                importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                importPipeline = androidImportPipeline,
                            )
                        }

                        if (iosEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = iosIcon,
                                importOutputName = parentName.sanitise().ToUpperCamelCase(),
                                importPipeline = iosImportPipeline,
                            )
                        }

                        if (webEnabled) {
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
