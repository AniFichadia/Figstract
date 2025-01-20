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
import com.anifichadia.figstract.ios.assetcatalog.Scale
import com.anifichadia.figstract.ios.assetcatalog.Type
import com.anifichadia.figstract.ios.figma.model.iosIcon
import com.anifichadia.figstract.ios.importer.asset.model.importing.assetCatalogFinalisationLifecycle
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
        val androidOutputDirectory = androidOutDirectory.fold("icons", "drawable")
        ImportPipeline(
            steps = androidSvgToAvd then
                androidVectorColorToPlaceholder then
                Destination.directoryDestination(androidOutputDirectory),
        )
    } else {
        null
    }

    val iosImportPipeline: ImportPipeline?
    val iosAssetCatalogLifecycle: Lifecycle
    if (iosOutDirectory != null) {
        val iosDirectory = File(iosOutDirectory, "icons")
        val assetCatalog = AssetCatalog(iosDirectory)

        iosImportPipeline = ImportPipeline(
            steps = iosStoreInAssetCatalog(
                assetCatalog = assetCatalog,
                contentName = "Icon",
                type = Type.Image.ImageSet,
                scale = Scale.`1x`,
            ),
        )
        iosAssetCatalogLifecycle = assetCatalogFinalisationLifecycle(assetCatalog)
    } else {
        iosImportPipeline = null
        iosAssetCatalogLifecycle = Lifecycle.NoOp
    }

    val webImportPipeline = if (webOutDirectory != null) {
        val webOutputDirectory = File(webOutDirectory, "icons")
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
        iosAssetCatalogLifecycle,
        timingLifecycle,
        timingLoggingLifecycle,
    )
    // Icons are smaller, so we can retrieve more at the same time
    val assetsPerChunk = 50

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
                        node.children.filterIsInstance<Node.Vector>().firstOrNull() ?: return@traverseBreadthFirst

                        if (!assetFilter.nodeNameFilter.accept(node)) return@traverseBreadthFirst
                        if (parent != null && !assetFilter.parentNameFilter.accept(parent)) return@traverseBreadthFirst

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
        }
    }
}
