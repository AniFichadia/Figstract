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
import com.anifichadia.figstract.importer.asset.model.exporting.pngUnscaled
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.AssetType
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.figma.model.ios3xImage
import com.anifichadia.figstract.ios.importer.asset.model.importing.HeicSupport
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosScaleAndStoreInAssetCatalog
import java.io.File

@Suppress("SameParameterValue")
internal fun createArtworkFigmaFileHandler(
    figmaFile: FileKey,
    figmaFileBranchName: String?,
    figmaFileVersion: String?,
    createUncropped: Boolean,
    createCropped: Boolean,
    androidOutDirectory: File?,
    iosOutDirectory: File?,
    webOutDirectory: File?,
    assetFilter: AssetFilter,
    androidNameGenerator: NodeTokenStringGenerator,
    iosNameGenerator: NodeTokenStringGenerator,
    webNameGenerator: NodeTokenStringGenerator,
    jsonPath: String?,
    androidExportConfig: ExportConfig = androidImageXxxHdpi,
    iosExportConfig: ExportConfig = ios3xImage,
    webExportConfig: ExportConfig = pngUnscaled,
    androidOutputDensityBuckets: List<DensityBucket> = DensityBucket.defaults,
    iosOutputScales: List<Scale> = Scale.defaults,
    iosConvertToHeic: Boolean = false,
    iosGroupByCanvas: Boolean = false,
    instructionLimit: Int? = null,
): AssetFileHandler {
    val androidImportPipeline = if (androidOutDirectory != null) {
        val androidOutputDirectory = File(androidOutDirectory, artworkDirectoryName)
        ImportPipeline(
            steps = androidImageScaleAndStoreInDensityBuckets(
                imageDirectory = androidOutputDirectory,
                sourceDensity = DensityBucket.XXXHDPI,
                densityBuckets = androidOutputDensityBuckets,
            ),
        )
    } else {
        null
    }

    val iosImportPipeline = if (iosOutDirectory != null) {
        val iosDirectory = File(iosOutDirectory, artworkDirectoryName)
        val assetCatalog = AssetCatalog(iosDirectory)

        if (iosConvertToHeic) {
            HeicSupport.requireAvailable()
        }

        ImportPipeline(
            steps = iosScaleAndStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                assetType = AssetType.Image.ImageSet,
                sourceScale = Scale.`3x`,
                scales = iosOutputScales,
                convertToHeic = iosConvertToHeic,
                groupByCanvas = iosGroupByCanvas,
            ),
        )
    } else {
        null
    }

    val webImportPipeline = if (webOutDirectory != null) {
        val webOutputDirectory = File(webOutDirectory, artworkDirectoryName)
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
        timingLifecycle,
        timingLoggingLifecycle,
    )

    return if (jsonPath == null) {
        AssetFileHandler(
            figmaFile = figmaFile,
            figmaFileBranchName = figmaFileBranchName,
            figmaFileVersion = figmaFileVersion,
            lifecycle = lifecycle,
        ) { response, _ ->
            val canvases = response.document.children
                .filterIsInstance<Node.Canvas>()
                .filter { canvas -> assetFilter.nodeNameFilter.accept(canvas) }

            canvases
                .map { canvas ->
                    Instruction.buildInstructions {
                        canvas.traverseBreadthFirst { node, parent ->
                            if (node !is Node.Parent) return@traverseBreadthFirst
                            val child = node.children.filterIsInstance<Node.Fillable>().firstOrNull()
                                ?: return@traverseBreadthFirst
                            if (!child.fills.any { it is Paint.Image }) return@traverseBreadthFirst

                            if (!assetFilter.nodeNameFilter.accept(node)) return@traverseBreadthFirst
                            if (parent != null && !assetFilter.parentNameFilter.accept(parent)) return@traverseBreadthFirst

                            val namingContext = NodeTokenStringGenerator.NodeContext(canvas, node)
                            val iosPathElements = if (iosGroupByCanvas) listOf(canvas.name) else emptyList()

                            fun addInstructions(
                                exportConfig: ExportConfig,
                                nameGenerator: NodeTokenStringGenerator,
                                importPipeline: ImportPipeline,
                                pathElements: List<String> = emptyList(),
                            ) {
                                if (createUncropped) {
                                    addInstruction(
                                        exportNode = node,
                                        exportConfig = exportConfig,
                                        importTarget = Instruction.ImportTarget.Initial(
                                            outputName = nameGenerator.generate(namingContext),
                                            pathElements = pathElements,
                                        ),
                                        importPipeline = importPipeline,
                                    )
                                }
                                if (createCropped) {
                                    addInstruction(
                                        exportNode = child,
                                        exportConfig = exportConfig,
                                        importTarget = Instruction.ImportTarget.Initial(
                                            outputName = nameGenerator.generate(namingContext, suffix = "cropped"),
                                            pathElements = pathElements,
                                        ),
                                        importPipeline = importPipeline,
                                    )
                                }
                            }

                            if (androidImportPipeline != null) {
                                addInstructions(
                                    exportConfig = androidExportConfig,
                                    nameGenerator = androidNameGenerator,
                                    importPipeline = androidImportPipeline,
                                )
                            }

                            if (iosImportPipeline != null) {
                                addInstructions(
                                    exportConfig = iosExportConfig,
                                    nameGenerator = iosNameGenerator,
                                    importPipeline = iosImportPipeline,
                                    pathElements = iosPathElements,
                                )
                            }

                            if (webImportPipeline != null) {
                                addInstructions(
                                    exportConfig = webExportConfig,
                                    nameGenerator = webNameGenerator,
                                    importPipeline = webImportPipeline,
                                )
                            }
                        }
                    }
                }
                .flatten()
                .run {
                    if (instructionLimit != null) {
                        this.take(instructionLimit)
                    } else {
                        this
                    }
                }
        }
    } else {
        JsonPathAssetFileHandler(
            figmaFile = figmaFile,
            figmaFileBranchName = figmaFileBranchName,
            figmaFileVersion = figmaFileVersion,
            jsonPath = jsonPath,
            lifecycle = lifecycle,
            canvasFilter = { canvas -> assetFilter.nodeNameFilter.accept(canvas) },
            nodeFilter = { node -> assetFilter.nodeNameFilter.accept(node) },
            instructionLimit = instructionLimit,
        ) { node, canvas ->
            Instruction.buildInstructions {
                val namingContext = NodeTokenStringGenerator.NodeContext(canvas, node)
                val iosPathElements = if (iosGroupByCanvas) listOf(canvas.name) else emptyList()

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
                        importTarget = Instruction.ImportTarget.Initial(
                            outputName = iosNameGenerator.generate(namingContext),
                            pathElements = iosPathElements,
                        ),
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

private const val artworkDirectoryName = "artwork"
