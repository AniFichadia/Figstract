package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.importer.asset.model.importing.androidSvgToAvd
import com.anifichadia.figstract.android.importer.asset.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figstract.cli.core.assets.AssetFilter
import com.anifichadia.figstract.cli.core.assets.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.timingLogger
import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.Instruction.Companion.addInstruction
import com.anifichadia.figstract.importer.asset.model.JsonPathAssetFileHandler
import com.anifichadia.figstract.importer.asset.model.exporting.svg
import com.anifichadia.figstract.importer.asset.model.importing.Destination
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figstract.ios.assetcatalog.AssetCatalog
import com.anifichadia.figstract.ios.assetcatalog.AssetType
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.figma.model.iosIcon
import com.anifichadia.figstract.ios.importer.asset.model.importing.iosStoreInAssetCatalog
import com.anifichadia.figstract.type.fold
import java.io.File

@Suppress("SameParameterValue")
internal fun createIconFigmaFileHandler(
    figmaFile: FileKey,
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
    // Icons are smaller, so we can retrieve more at the same time
    val assetsPerChunk = 50

    fun MutableList<Instruction>.generateInstructions(canvas: Node.Canvas, node: Node) {
        val namingContext = NodeTokenStringGenerator.NodeContext(canvas, node)

        if (androidImportPipeline != null) {
            addInstruction(
                exportNode = node,
                exportConfig = svg,
                importOutputName = androidNameGenerator.generate(namingContext),
                importPipeline = androidImportPipeline,
            )
        }

        if (iosImportPipeline != null) {
            addInstruction(
                exportNode = node,
                exportConfig = iosIcon,
                importOutputName = iosNameGenerator.generate(namingContext),
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

    return if (jsonPath == null) {
        AssetFileHandler(
            figmaFile = figmaFile,
            assetsPerChunk = assetsPerChunk,
            lifecycle = lifecycle,
        ) { response, _ ->
            val canvases = response.document.children
                .filterIsInstance<Node.Canvas>()
                .filter { canvas -> assetFilter.canvasNameFilter.accept(canvas) }

            canvases.map { canvas ->
                Instruction.buildInstructions {
                    canvas.traverseBreadthFirst { node, parent ->
                        if (node !is Node.Parent) return@traverseBreadthFirst
                        // Look for first descendants of the canvas
                        if (parent !== canvas) return@traverseBreadthFirst

                        node.children.filterIsInstance<Node.Vector>().firstOrNull() ?: return@traverseBreadthFirst

                        if (!assetFilter.nodeNameFilter.accept(node)) return@traverseBreadthFirst
                        if (!assetFilter.parentNameFilter.accept(parent)) return@traverseBreadthFirst

                        val nodeToExport = node as? Node.Component ?: return@traverseBreadthFirst

                        generateInstructions(canvas, nodeToExport)
                    }
                }
            }.flatten()
        }
    } else {
        JsonPathAssetFileHandler(
            figmaFile = figmaFile,
            jsonPath = jsonPath,
            assetsPerChunk = assetsPerChunk,
            lifecycle = lifecycle,
            canvasFilter = { canvas -> assetFilter.nodeNameFilter.accept(canvas) },
            nodeFilter = { node -> assetFilter.nodeNameFilter.accept(node) },
        ) { node, canvas ->
            Instruction.buildInstructions {
                generateInstructions(canvas, node)
            }
        }
    }
}

private const val iconsDirectoryName = "icons"
