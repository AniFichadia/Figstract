package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figstract.importer.Lifecycle

/**
 * Builds an [AssetFileHandler] from the explicit steps shared by every asset extraction handler:
 * 1. Select canvases from the document, via [assetFilter]'s [AssetFilter.canvasNameFilter].
 * 2. Discover candidate nodes within each selected canvas, via [discoveryStrategy].
 * 3. Filter candidates by name, via [assetFilter]'s [AssetFilter.nodeNameFilter].
 * 4. Filter candidates by parent name, via [assetFilter]'s [AssetFilter.parentNameFilter].
 * 5. Build instructions for each surviving `(canvas, node)` pair, via [createInstructions].
 * 6. Optionally cap the resulting instruction count, via [instructionLimit].
 * 7. Report which canvas/node names were actually seen, via [seenNameTracker].
 */
@Suppress("FunctionName")
fun FigmaAssetFileHandler(
    figmaFileDefinition: FigmaFileDefinition,
    discoveryStrategy: NodeDiscoveryStrategy,
    assetFilter: AssetFilter,
    assetsPerChunk: Int = AssetFileHandler.DEFAULT_ASSETS_PER_CHUNK,
    lifecycle: Lifecycle = Lifecycle.NoOp,
    instructionLimit: Int? = null,
    seenNameTracker: SeenNameTracker = SeenNameTracker.NoOp,
    createInstructions: (canvas: Node.Canvas, node: Node) -> List<Instruction>,
) = AssetFileHandler(
    figmaFileDefinition = figmaFileDefinition,
    assetsPerChunk = assetsPerChunk,
    lifecycle = lifecycle,
) { response, _ ->
    val canvases = response
        .document
        .children
        .filterIsInstance<Node.Canvas>()
        .filter { canvas -> assetFilter.canvasNameFilter.accept(canvas) }

    val seenCanvasNames = mutableSetOf<String>()
    val seenNodeNames = mutableSetOf<String>()

    canvases
        .flatMap { canvas ->
            seenCanvasNames += canvas.name

            val candidates = discoveryStrategy
                .discover(canvas)
                .filter { node -> assetFilter.nodeNameFilter.accept(node) }

            // Re-traverse the canvas to recover each candidate's parent, so parentNameFilter can be applied regardless
            // of discoveryStrategy
            val candidateIds = candidates.map { node -> node.id }.toSet()
            val filteredCandidates = mutableSetOf<String>()
            canvas.traverseBreadthFirst { node, parent ->
                if (node.id !in candidateIds) return@traverseBreadthFirst

                if (parent == null || assetFilter.parentNameFilter.accept(parent)) {
                    filteredCandidates += node.id
                }
            }

            candidates
                .filter { node -> node.id in filteredCandidates }
                .flatMap { node ->
                    seenNodeNames += node.name
                    createInstructions(canvas, node)
                }
        }
        .run {
            if (instructionLimit != null) {
                this.take(instructionLimit)
            } else {
                this
            }
        }
        .also {
            seenNameTracker.report(seenCanvasNames, seenNodeNames)
        }
}
