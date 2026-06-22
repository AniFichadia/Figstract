package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figstract.android.importer.asset.model.drawable.DensityBucket
import com.anifichadia.figstract.android.importer.asset.model.importing.androidImageScaleAndStoreInDensityBuckets
import com.anifichadia.figstract.cli.core.timingLogger
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.figma.model.Paint
import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.AssetFilter
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import com.anifichadia.figstract.importer.asset.model.FigmaAssetFileHandler
import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.Instruction.Companion.addInstruction
import com.anifichadia.figstract.importer.asset.model.JsonPath
import com.anifichadia.figstract.importer.asset.model.NodeDiscoveryStrategy
import com.anifichadia.figstract.importer.asset.model.NodeTokenStringGenerator
import com.anifichadia.figstract.importer.asset.model.TraverseBreadthFirst
import com.anifichadia.figstract.importer.asset.model.asSeenNameTracker
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figstract.importer.asset.model.exporting.pngUnscaled
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.toNamingContext
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.AssetType
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.figma.model.ios3xImage
import com.anifichadia.figstract.ios.importer.asset.model.importing.ArtworkOutputFormat
import com.anifichadia.figstract.ios.importer.asset.model.importing.HeicSupport
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosScaleAndStoreInAssetCatalog
import java.io.File

@Suppress("SameParameterValue")
internal fun createArtworkFigmaFileHandler(
    figmaFileDefinition: FigmaFileDefinition,
    createUncropped: Boolean,
    createCropped: Boolean,
    androidOutDirectory: File?,
    iosOutDirectory: File?,
    webOutDirectory: File?,
    assetFilter: AssetFilter,
    renamingMap: AssetRenamingMap,
    androidNameGenerator: NodeTokenStringGenerator,
    iosNameGenerator: NodeTokenStringGenerator,
    webNameGenerator: NodeTokenStringGenerator,
    jsonPath: String?,
    androidExportConfig: ExportConfig = androidImageXxxHdpi,
    iosExportConfig: ExportConfig = ios3xImage,
    webExportConfig: ExportConfig = pngUnscaled,
    androidOutputDensityBuckets: List<DensityBucket> = DensityBucket.defaults,
    iosOutputScales: List<Scale> = Scale.defaults,
    iosOutputFormat: ArtworkOutputFormat = ArtworkOutputFormat.Default,
    iosGroupByToken: NodeTokenStringGenerator? = null,
    instructionLimit: Int? = null,
): AssetFileHandler {
    //region Import pipelines
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

        if (iosOutputFormat == ArtworkOutputFormat.Heic) {
            HeicSupport.requireAvailable()
        }

        ImportPipeline(
            steps = iosScaleAndStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                assetType = AssetType.Image.ImageSet,
                sourceScale = Scale.`3x`,
                scales = iosOutputScales,
                outputFormat = iosOutputFormat,
                groupByPathElements = iosGroupByToken != null,
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
    //endregion

    //region Lifecycles
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
    //endregion

    // A node is a candidate if it's a Parent with a Fillable child carrying an image fill.
    val discoveryStrategy = if (jsonPath == null) {
        NodeDiscoveryStrategy.TraverseBreadthFirst { node, parent ->
            if (node !is Node.Parent) return@TraverseBreadthFirst false
            val child = node.children.filterIsInstance<Node.Fillable>().firstOrNull()
                ?: return@TraverseBreadthFirst false
            if (!child.fills.any { it is Paint.Image }) return@TraverseBreadthFirst false

            true
        }
    } else {
        NodeDiscoveryStrategy.JsonPath(jsonPath)
    }

    // Looks up the same Fillable child the discovery predicate matched against, to export the "cropped" variant.
    // This is a pure function of node, so it's safe to recompute here rather than threading it through discovery.
    fun croppedChildOf(node: Node): Node? =
        (node as? Node.Parent)?.children?.filterIsInstance<Node.Fillable>()?.firstOrNull()

    return FigmaAssetFileHandler(
        figmaFileDefinition = figmaFileDefinition,
        discoveryStrategy = discoveryStrategy,
        assetFilter = assetFilter,
        lifecycle = lifecycle,
        instructionLimit = instructionLimit,
        seenNameTracker = renamingMap.asSeenNameTracker(),
    ) { canvas, node ->
        Instruction.buildInstructions {
            val croppedChild = croppedChildOf(node)

            val namingContext = renamingMap.toNamingContext(canvas, node)

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
                if (createCropped && croppedChild != null) {
                    addInstruction(
                        exportNode = croppedChild,
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
                val iosPathElements = if (iosGroupByToken != null) {
                    listOf(iosGroupByToken.generate(namingContext))
                } else {
                    emptyList()
                }

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

private const val artworkDirectoryName = "artwork"
