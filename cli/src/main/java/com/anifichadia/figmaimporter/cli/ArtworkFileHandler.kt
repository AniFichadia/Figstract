package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figmaimporter.android.model.drawable.DensityBucket
import com.anifichadia.figmaimporter.android.model.importing.androidImageScaleAndStoreInDensityBuckets
import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figmaimporter.figma.model.Paint
import com.anifichadia.figmaimporter.ios.figma.model.ios3xImage
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.model.assetcatalog.createAssetCatalogContentDirectory
import com.anifichadia.figmaimporter.ios.model.assetcatalog.createAssetCatalogRootDirectory
import com.anifichadia.figmaimporter.ios.model.importing.assetCatalogFinalisationLifecycle
import com.anifichadia.figmaimporter.ios.model.importing.iosScaleAndStoreInAssetCatalog
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.Instruction
import com.anifichadia.figmaimporter.model.Instruction.Companion.addInstruction
import com.anifichadia.figmaimporter.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.model.importing.Destination
import com.anifichadia.figmaimporter.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.util.sanitise
import com.anifichadia.figmaimporter.util.to_snake_case
import java.io.File

@Suppress("SameParameterValue")
internal fun createArtworkFigmaFileHandler(
    figmaFile: FileKey,
    createCropped: Boolean,
    androidOutDirectory: File,
    iosOutDirectory: File,
    webOutDirectory: File,
    androidEnabled: Boolean,
    iosEnabled: Boolean,
    webEnabled: Boolean,
    assetFilter: AssetFilter,
    instructionLimit: Int?,
): FigmaFileHandler {
    val androidOutputDirectory = File(androidOutDirectory, "artwork")
    val androidImportPipeline = ImportPipeline(
        steps = androidImageScaleAndStoreInDensityBuckets(androidOutputDirectory, DensityBucket.XXXHDPI),
        // Destination is handled by the steps
        destination = Destination.None,
    )

    val iosOutputDirectory = File(iosOutDirectory, "artwork")
    val iosAssetCatalogRootDirectory = createAssetCatalogRootDirectory(iosOutputDirectory)
    val iosContentDirectory = createAssetCatalogContentDirectory(iosAssetCatalogRootDirectory, "Images")
    val iosImportPipeline = ImportPipeline(
        steps = iosScaleAndStoreInAssetCatalog(iosContentDirectory, Type.IMAGE_SET, Scale.`3x`),
        // Destination is handled by the steps
        destination = Destination.None,
    )
    val iosAssetCatalogLifecycle = if (iosEnabled) {
        assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory)
    } else {
        FigmaFileHandler.Lifecycle.NoOp
    }

    val webOutputDirectory = File(webOutDirectory, "artwork")
    val webImportPipeline = ImportPipeline(
        destination = Destination.directoryDestination(webOutputDirectory),
    )

    val timingLifecycle = FigmaFileHandler.Lifecycle.Timing()
    val timingLoggingLifecycle = object : FigmaFileHandler.Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Artwork retrieval timing: \n$timingLifecycle" }
        }
    }

    val artworkFileHandler = FigmaFileHandler(
        figmaFile = figmaFile,
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
            val canvasName = canvas.name
            Instruction.buildInstructions {
                canvas.traverseBreadthFirst { node, parent ->
                    if (node.id !in excludedNodes && parent != null && node is Node.Fillable && node.fills.any { it is Paint.Image }) {
                        val parentName = parent.name

                        if (androidEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = androidImageXxxHdpi,
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = androidImportPipeline,
                            )
                            if (createCropped) {
                                addInstruction(
                                    exportNodeId = node.id,
                                    exportConfig = androidImageXxxHdpi,
                                    importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                        .sanitise()
                                        .to_snake_case(),
                                    importPipeline = androidImportPipeline,
                                )
                            }
                        }

                        if (iosEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = ios3xImage,
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = iosImportPipeline,
                            )
                            if (createCropped) {
                                addInstruction(
                                    exportNodeId = node.id,
                                    exportConfig = ios3xImage,
                                    importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                        .sanitise()
                                        .to_snake_case(),
                                    importPipeline = iosImportPipeline,
                                )
                            }
                        }

                        if (webEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = webImportPipeline,
                            )
                            if (createCropped) {
                                addInstruction(
                                    exportNodeId = node.id,
                                    exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                    importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                        .sanitise()
                                        .to_snake_case(),
                                    importPipeline = webImportPipeline,
                                )
                            }
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

    return artworkFileHandler
}
