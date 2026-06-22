package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.android.importer.asset.model.importing.androidSvgToAvd
import com.anifichadia.figstract.android.importer.asset.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figstract.cli.core.assets.model.AssetFilter
import com.anifichadia.figstract.cli.core.assets.model.AssetRenamingMap
import com.anifichadia.figstract.cli.core.assets.model.NodeTokenStringGenerator
import com.anifichadia.figstract.cli.core.timingLogger
import com.anifichadia.figstract.figma.FigmaFileDefinition
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

    fun MutableList<Instruction>.generateInstructions(canvas: Node.Canvas, node: Node) {
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

    // Icons are smaller, so we can retrieve more at the same time
    val assetsPerChunk = 50

    return if (jsonPath == null) {
        AssetFileHandler(
            figmaFileDefinition = figmaFileDefinition,
            assetsPerChunk = assetsPerChunk,
            lifecycle = lifecycle,
        ) { response, _ ->
            val canvases = response.document.children
                .filterIsInstance<Node.Canvas>()
                .filter { canvas -> assetFilter.canvasNameFilter.accept(canvas) }

            val seenCanvasNames = mutableSetOf<String>()
            val seenNodeNames = mutableSetOf<String>()

            canvases
                .flatMap { canvas ->
                    seenCanvasNames += canvas.name

                    Instruction.buildInstructions {
                        canvas.traverseBreadthFirst { node, parent ->
                            if (node !is Node.Parent) return@traverseBreadthFirst
                            // Look for first descendants of the canvas
                            if (parent !== canvas) return@traverseBreadthFirst

                            node.children.filterIsInstance<Node.Vector>().firstOrNull() ?: return@traverseBreadthFirst

                            if (!assetFilter.nodeNameFilter.accept(node)) return@traverseBreadthFirst
                            if (!assetFilter.parentNameFilter.accept(parent)) return@traverseBreadthFirst

                            val nodeToExport = node as? Node.Component ?: return@traverseBreadthFirst

                            seenNodeNames += node.name

                            generateInstructions(canvas, nodeToExport)
                        }
                    }
                }
                .run {
                    renamingMap.warnUnused(seenCanvasNames, seenNodeNames)

                    if (instructionLimit != null) {
                        this.take(instructionLimit)
                    } else {
                        this
                    }
                }
        }
    } else {
        val seenCanvasNames = mutableSetOf<String>()
        val seenNodeNames = mutableSetOf<String>()

        JsonPathAssetFileHandler(
            figmaFileDefinition = figmaFileDefinition,
            jsonPath = jsonPath,
            assetsPerChunk = assetsPerChunk,
            lifecycle = lifecycle,
            canvasFilter = { canvas -> assetFilter.nodeNameFilter.accept(canvas) },
            nodeFilter = { node -> assetFilter.nodeNameFilter.accept(node) },
            instructionLimit = instructionLimit,
            onInstructionsCreated = {
                renamingMap.warnUnused(seenCanvasNames, seenNodeNames)
            },
        ) { node, canvas ->
            Instruction.buildInstructions {
                seenCanvasNames += canvas.name
                seenNodeNames += node.name

                generateInstructions(canvas, node)
            }
        }
    }
}

private const val iconsDirectoryName = "icons"
