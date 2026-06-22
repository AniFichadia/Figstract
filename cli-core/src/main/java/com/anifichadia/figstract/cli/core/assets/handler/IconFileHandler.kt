package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.importer.asset.model.importing.androidSvgToAvd
import com.anifichadia.figstract.android.importer.asset.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figstract.cli.core.timingLogger
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.figma.model.Node
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
import com.anifichadia.figstract.importer.asset.model.exporting.svg
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figstract.importer.asset.model.toNamingContext
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.AssetType
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.figma.model.iosIcon
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosStoreInAssetCatalog
import com.anifichadia.figstract.type.fold
import java.io.File

@Suppress("SameParameterValue")
internal fun createIconFigmaFileHandler(
    figmaFileDefinition: FigmaFileDefinition,
    androidOutDirectory: File?,
    iosOutDirectory: File?,
    webOutDirectory: File?,
    assetFilter: AssetFilter,
    renamingMap: AssetRenamingMap,
    androidNameGenerator: NodeTokenStringGenerator,
    iosNameGenerator: NodeTokenStringGenerator,
    webNameGenerator: NodeTokenStringGenerator,
    jsonPath: String?,
    iosGroupByToken: NodeTokenStringGenerator? = null,
    instructionLimit: Int? = null,
): AssetFileHandler {
    //region Import pipelines
    val androidImportPipeline = if (androidOutDirectory != null) {
        val androidOutputDirectory = androidOutDirectory.fold(iconsDirectoryName, "drawable")
        ImportPipeline(
            steps = androidSvgToAvd then
                androidVectorColorToPlaceholder then
                Destination.directoryDestination(androidOutputDirectory),
        )
    } else {
        null
    }

    val iosImportPipeline = if (iosOutDirectory != null) {
        val iosDirectory = File(iosOutDirectory, iconsDirectoryName)
        val assetCatalog = AssetCatalog(iosDirectory)

        ImportPipeline(
            steps = iosStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                assetType = AssetType.Image.ImageSet,
                scale = Scale.`1x`,
                groupByPathElements = iosGroupByToken != null,
            ),
        )
    } else {
        null
    }

    val webImportPipeline = if (webOutDirectory != null) {
        val webOutputDirectory = File(webOutDirectory, iconsDirectoryName)
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
            timingLogger.info { "Icon retrieval timing: \n$timingLifecycle" }
        }
    }

    val lifecycle = Lifecycle.Combined(
        timingLifecycle,
        timingLoggingLifecycle,
    )
    //endregion

    // A node is a candidate if it's a direct child of the canvas, a Component, and has a Vector child.
    val discoveryStrategy = if (jsonPath == null) {
        NodeDiscoveryStrategy.TraverseBreadthFirst { node, parent ->
            if (node !is Node.Parent) return@TraverseBreadthFirst false
            // Look for first descendants of the canvas. Traversal runs once per canvas, so a
            // Node.Canvas parent here can only be the canvas currently being traversed.
            if (parent !is Node.Canvas) return@TraverseBreadthFirst false

            node.children.filterIsInstance<Node.Vector>().firstOrNull() ?: return@TraverseBreadthFirst false

            if (node !is Node.Component) return@TraverseBreadthFirst false

            true
        }
    } else {
        NodeDiscoveryStrategy.JsonPath(jsonPath)
    }

    // Icons are smaller, so we can retrieve more at the same time
    val assetsPerChunk = 50

    return FigmaAssetFileHandler(
        figmaFileDefinition = figmaFileDefinition,
        discoveryStrategy = discoveryStrategy,
        assetFilter = assetFilter,
        assetsPerChunk = assetsPerChunk,
        lifecycle = lifecycle,
        instructionLimit = instructionLimit,
        seenNameTracker = renamingMap.asSeenNameTracker(),
    ) { canvas, node ->
        Instruction.buildInstructions {
            val namingContext = renamingMap.toNamingContext(canvas, node)

            if (androidImportPipeline != null) {
                addInstruction(
                    exportNode = node,
                    exportConfig = svg,
                    importOutputName = androidNameGenerator.generate(namingContext),
                    importPipeline = androidImportPipeline,
                )
            }

            if (iosImportPipeline != null) {
                val iosPathElements = if (iosGroupByToken != null) {
                    listOf(iosGroupByToken.generate(namingContext))
                } else {
                    emptyList()
                }

                addInstruction(
                    exportNode = node,
                    exportConfig = iosIcon,
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
                    exportConfig = svg,
                    importOutputName = webNameGenerator.generate(namingContext),
                    importPipeline = webImportPipeline,
                )
            }
        }
    }
}

private const val iconsDirectoryName = "icons"
