package com.anifichadia.figmaimporter.cli.core.assets.handler

import com.anifichadia.figmaimporter.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figmaimporter.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figmaimporter.android.importer.asset.model.importing.androidImageScaleAndStoreInDensityBuckets
import com.anifichadia.figmaimporter.cli.core.assets.AssetFilter
import com.anifichadia.figmaimporter.cli.core.timingLogger
import com.anifichadia.figmaimporter.figma.FileKey
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figmaimporter.figma.model.Paint
import com.anifichadia.figmaimporter.importer.asset.model.AssetFileHandler
import com.anifichadia.figmaimporter.importer.asset.model.Instruction
import com.anifichadia.figmaimporter.importer.asset.model.Instruction.Companion.addInstruction
import com.anifichadia.figmaimporter.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.importer.asset.model.importing.Destination
import com.anifichadia.figmaimporter.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.ios.figma.model.ios3xImage
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.createAssetCatalogContentDirectory
import com.anifichadia.figmaimporter.ios.importer.asset.model.assetcatalog.createAssetCatalogRootDirectory
import com.anifichadia.figmaimporter.ios.importer.asset.model.importing.assetCatalogFinalisationLifecycle
import com.anifichadia.figmaimporter.ios.importer.asset.model.importing.iosScaleAndStoreInAssetCatalog
import com.anifichadia.figmaimporter.util.sanitise
import com.anifichadia.figmaimporter.util.to_snake_case
import java.io.File

@Suppress("SameParameterValue")
internal fun createArtworkFigmaFileHandler(
    figmaFile: FileKey,
    createCropped: Boolean,
    androidOutDirectory: File?,
    iosOutDirectory: File?,
    webOutDirectory: File?,
    assetFilter: AssetFilter,
    instructionLimit: Int?,
): AssetFileHandler {
    val androidImportPipeline = if (androidOutDirectory != null) {
        val androidOutputDirectory = File(androidOutDirectory, "artwork")
        ImportPipeline(
            steps = androidImageScaleAndStoreInDensityBuckets(androidOutputDirectory, DensityBucket.XXXHDPI),
            // Destination is handled by the steps
            destination = Destination.None,
        )
    } else {
        null
    }

    val iosImportPipeline: ImportPipeline?
    val iosAssetCatalogLifecycle: AssetFileHandler.Lifecycle
    if (iosOutDirectory != null) {
        val iosOutputDirectory = File(iosOutDirectory, "artwork")
        val iosAssetCatalogRootDirectory = createAssetCatalogRootDirectory(iosOutputDirectory)
        val iosContentDirectory = createAssetCatalogContentDirectory(iosAssetCatalogRootDirectory, "Images")

        iosImportPipeline = ImportPipeline(
            steps = iosScaleAndStoreInAssetCatalog(iosContentDirectory, Type.IMAGE_SET, Scale.`3x`),
            // Destination is handled by the steps
            destination = Destination.None,
        )
        iosAssetCatalogLifecycle = assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory)
    } else {
        iosImportPipeline = null
        iosAssetCatalogLifecycle = AssetFileHandler.Lifecycle.NoOp
    }

    val webImportPipeline = if (webOutDirectory != null) {
        val webOutputDirectory = File(webOutDirectory, "artwork")
        ImportPipeline(
            destination = Destination.directoryDestination(webOutputDirectory),
        )
    } else {
        null
    }

    val timingLifecycle = AssetFileHandler.Lifecycle.Timing()
    val timingLoggingLifecycle = object : AssetFileHandler.Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Artwork retrieval timing: \n$timingLifecycle" }
        }
    }

    val artworkFileHandler = AssetFileHandler(
        figmaFile = figmaFile,
        lifecycle = AssetFileHandler.Lifecycle.Combined(
            iosAssetCatalogLifecycle,
            timingLifecycle,
            timingLoggingLifecycle,
        ),
    ) { response, _ ->
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()
            .filter { canvas -> assetFilter.nodeNameFilter.accept(canvas) }

        canvases.map { canvas ->
            val canvasName = canvas.name
            Instruction.buildInstructions {
                canvas.traverseBreadthFirst { node, parent ->
                    if (parent == null) return@traverseBreadthFirst
                    if (node !is Node.Fillable) return@traverseBreadthFirst
                    if (!node.fills.any { it is Paint.Image }) return@traverseBreadthFirst

                    if (!assetFilter.nodeNameFilter.accept(node)) return@traverseBreadthFirst
                    if (!assetFilter.parentNameFilter.accept(parent)) return@traverseBreadthFirst

                    val parentName = parent.name

                    if (androidImportPipeline != null) {
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

                    if (iosImportPipeline != null) {
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

                    if (webImportPipeline != null) {
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
