package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.android.importer.asset.model.importing.androidImageScaleAndStoreInDensityBuckets
import com.anifichadia.figstract.cli.core.assets.AssetFilter
import com.anifichadia.figstract.cli.core.assets.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.timingLogger
import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figstract.figma.model.Paint
import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.Instruction.Companion.addInstruction
import com.anifichadia.figstract.importer.asset.model.JsonPathAssetFileHandler
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.assetcatalog.Type
import com.anifichadia.figstract.ios.figma.model.ios3xImage
import com.anifichadia.figstract.ios.importer.asset.model.importing.assetCatalogFinalisationLifecycle
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosScaleAndStoreInAssetCatalog
import java.io.File

@Suppress("SameParameterValue")
internal fun createArtworkFigmaFileHandler(
    figmaFile: FileKey,
    createCropped: Boolean,
    androidOutDirectory: File?,
    iosOutDirectory: File?,
    webOutDirectory: File?,
    assetFilter: AssetFilter,
    androidNameGenerator: NodeTokenStringGenerator,
    iosNameGenerator: NodeTokenStringGenerator,
    webNameGenerator: NodeTokenStringGenerator,
    jsonPath: String?,
): AssetFileHandler {
    val androidImportPipeline = if (androidOutDirectory != null) {
        val androidOutputDirectory = File(androidOutDirectory, "artwork")
        ImportPipeline(
            steps = androidImageScaleAndStoreInDensityBuckets(androidOutputDirectory, DensityBucket.XXXHDPI),
        )
    } else {
        null
    }

    val iosImportPipeline: ImportPipeline?
    val iosAssetCatalogLifecycle: Lifecycle
    if (iosOutDirectory != null) {
        val iosDirectory = File(iosOutDirectory, "artwork")
        val assetCatalog = AssetCatalog(iosDirectory)

        iosImportPipeline = ImportPipeline(
            steps = iosScaleAndStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                contentName = "Images",
                type = Type.IMAGE_SET,
                sourceScale = Scale.`3x`,
            ),
        )
        iosAssetCatalogLifecycle = assetCatalogFinalisationLifecycle(assetCatalog)
    } else {
        iosImportPipeline = null
        iosAssetCatalogLifecycle = Lifecycle.NoOp
    }

    val webImportPipeline = if (webOutDirectory != null) {
        val webOutputDirectory = File(webOutDirectory, "artwork")
        ImportPipeline(
            steps = Destination.directoryDestination(webOutputDirectory),
        )
    } else {
        null
    }

    val timingLifecycle = Lifecycle.Timing()
    val timingLoggingLifecycle = object : Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Artwork retrieval timing: \n$timingLifecycle" }
        }
    }

    val lifecycle = Lifecycle.Combined(
        iosAssetCatalogLifecycle,
        timingLifecycle,
        timingLoggingLifecycle,
    )

    return if (jsonPath == null) {
        AssetFileHandler(
            figmaFile = figmaFile,
            lifecycle = lifecycle,
        ) { response, _ ->
            val canvases = response.document.children
                .filterIsInstance<Node.Canvas>()
                .filter { canvas -> assetFilter.nodeNameFilter.accept(canvas) }

            canvases.map { canvas ->
                Instruction.buildInstructions {
                    canvas.traverseBreadthFirst { node, parent ->
                        if (node !is Node.Parent) return@traverseBreadthFirst
                        val child = node.children.filterIsInstance<Node.Fillable>().firstOrNull()
                            ?: return@traverseBreadthFirst
                        if (!child.fills.any { it is Paint.Image }) return@traverseBreadthFirst

                        if (!assetFilter.nodeNameFilter.accept(node)) return@traverseBreadthFirst
                        if (parent != null && !assetFilter.parentNameFilter.accept(parent)) return@traverseBreadthFirst

                        val namingContext = NodeTokenStringGenerator.NodeContext(canvas, node)

                        if (androidImportPipeline != null) {
                            addInstruction(
                                exportNode = node,
                                exportConfig = androidImageXxxHdpi,
                                importOutputName = androidNameGenerator.generate(namingContext),
                                importPipeline = androidImportPipeline,
                            )
                            if (createCropped) {
                                addInstruction(
                                    exportNode = child,
                                    exportConfig = androidImageXxxHdpi,
                                    importOutputName = androidNameGenerator.generate(namingContext, suffix = "cropped"),
                                    importPipeline = androidImportPipeline,
                                )
                            }
                        }

                        if (iosImportPipeline != null) {
                            addInstruction(
                                exportNode = node,
                                exportConfig = ios3xImage,
                                importOutputName = iosNameGenerator.generate(namingContext),
                                importPipeline = iosImportPipeline,
                            )
                            if (createCropped) {
                                addInstruction(
                                    exportNode = child,
                                    exportConfig = ios3xImage,
                                    importOutputName = iosNameGenerator.generate(namingContext, suffix = "cropped"),
                                    importPipeline = iosImportPipeline,
                                )
                            }
                        }

                        if (webImportPipeline != null) {
                            addInstruction(
                                exportNode = node,
                                exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                importOutputName = webNameGenerator.generate(namingContext),
                                importPipeline = webImportPipeline,
                            )
                            if (createCropped) {
                                addInstruction(
                                    exportNode = child,
                                    exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                    importOutputName = webNameGenerator.generate(namingContext, suffix = "cropped"),
                                    importPipeline = webImportPipeline,
                                )
                            }
                        }
                    }
                }
            }.flatten()
        }
    } else {
        JsonPathAssetFileHandler(
            figmaFile = figmaFile,
            jsonPath = jsonPath,
            lifecycle = lifecycle,
            canvasFilter = { canvas -> assetFilter.nodeNameFilter.accept(canvas) },
            nodeFilter = { node -> assetFilter.nodeNameFilter.accept(node) },
        ) { node, canvas ->
            Instruction.buildInstructions {
                val namingContext = NodeTokenStringGenerator.NodeContext(canvas, node)

                if (androidImportPipeline != null) {
                    addInstruction(
                        exportNode = node,
                        exportConfig = androidImageXxxHdpi,
                        importOutputName = androidNameGenerator.generate(namingContext),
                        importPipeline = androidImportPipeline,
                    )
                }

                if (iosImportPipeline != null) {
                    addInstruction(
                        exportNode = node,
                        exportConfig = ios3xImage,
                        importOutputName = iosNameGenerator.generate(namingContext),
                        importPipeline = iosImportPipeline,
                    )
                }

                if (webImportPipeline != null) {
                    addInstruction(
                        exportNode = node,
                        exportConfig = ExportConfig(ExportSetting.Format.PNG),
                        importOutputName = webNameGenerator.generate(namingContext),
                        importPipeline = webImportPipeline,
                    )
                }
            }
        }
    }
}
