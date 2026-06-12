package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.assets.AssetRenamingMap
import com.anifichadia.figstract.cli.core.assets.NodeTokenStringGenerator
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.util.createLogger

private val renamingLogger = createLogger("AssetRenaming")

/**
 * Produces a [NodeTokenStringGenerator.NodeContext] with canvas and node names remapped according to this [AssetRenamingMap].
 *
 * The canvas is remapped via [Node.Canvas.copy], which is safe as it's a data class.
 *
 * The node name is remapped via [Node.withName], which copies the concrete subtype with the new name. This keeps the
 * naming context honest about what name will appear in generated output without touching the real node used for export
 * and type checks.
 *
 * Logs a warning for any canvas or node entries in this [AssetRenamingMap] that did not match any node encountered
 * during traversal.
 */
fun AssetRenamingMap.toNamingContext(canvas: Node.Canvas, node: Node): NodeTokenStringGenerator.NodeContext {
    val renamedCanvas = canvases[canvas.name]?.let { canvas.copy(name = it) } ?: canvas
    val renamedNode = nodes[node.name]?.let { newName -> node.withName(newName) } ?: node
    return NodeTokenStringGenerator.NodeContext(canvas = renamedCanvas, node = renamedNode)
}

/**
 * Logs warnings for any entries in this [AssetRenamingMap] whose keys were never matched against
 * the provided [seenCanvasNames] and [seenNodeNames]. Call this after traversal is complete.
 */
fun AssetRenamingMap.warnUnused(seenCanvasNames: Set<String>, seenNodeNames: Set<String>) {
    val unusedCanvases = canvases.keys - seenCanvasNames
    val unusedNodes = nodes.keys - seenNodeNames

    if (unusedCanvases.isNotEmpty()) {
        renamingLogger.warn {
            "Renaming map contains canvas entries that did not match any canvas: $unusedCanvases"
        }
    }
    if (unusedNodes.isNotEmpty()) {
        renamingLogger.warn {
            "Renaming map contains node entries that did not match any node: $unusedNodes"
        }
    }
}

private fun Node.withName(newName: String): Node = when (this) {
    is Node.BooleanOperation -> this.copy(name = newName)
    is Node.Canvas -> this.copy(name = newName)
    is Node.Component -> this.copy(name = newName)
    is Node.ComponentSet -> this.copy(name = newName)
    is Node.Connector -> this.copy(name = newName)
    is Node.Document -> this.copy(name = newName)
    is Node.Ellipse -> this.copy(name = newName)
    is Node.Frame -> this.copy(name = newName)
    is Node.Group -> this.copy(name = newName)
    is Node.Instance -> this.copy(name = newName)
    is Node.Line -> this.copy(name = newName)
    is Node.Rectangle -> this.copy(name = newName)
    is Node.RegularPolygon -> this.copy(name = newName)
    is Node.Section -> this.copy(name = newName)
    is Node.ShapeWithText -> this.copy(name = newName)
    is Node.Slice -> this.copy(name = newName)
    is Node.Star -> this.copy(name = newName)
    is Node.Sticky -> this.copy(name = newName)
    is Node.Table -> this.copy(name = newName)
    is Node.TableCell -> this.copy(name = newName)
    is Node.Text -> this.copy(name = newName)
    is Node.Vector -> this.copy(name = newName)
    is Node.WashiTape -> this.copy(name = newName)
    is Node.Fillable,
    is Node.Parent,
    is Node.Placeable,
        -> error("${this::class.qualifiedName} cannot be renamed")
}
