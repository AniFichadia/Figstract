package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figstract.figma.model.Node.Companion.traverseDepthFirst


/**
 * Breadth-first traversal from [Node.Canvas], yielding every node for which [predicate] returns `true`.
 *
 * [predicate] receives the candidate node and its immediate parent, and is responsible for any node-shape checks
 * (e.g. "is a [Node.Parent] with a [Node.Fillable] child that has an image fill").
 */
@Suppress("FunctionName")
fun NodeDiscoveryStrategy.Companion.TraverseBreadthFirst(
    predicate: (node: Node, parent: Node.Parent?) -> Boolean,
): NodeDiscoveryStrategy = NodeDiscoveryStrategy { canvas ->
    buildList {
        canvas.traverseBreadthFirst { node, parent ->
            if (predicate(node, parent)) add(node)
        }
    }
}

/**
 * Depth-first traversal from [Node.Canvas], yielding every node for which [predicate] returns `true`.
 *
 * [predicate] receives the candidate node and its immediate parent, and is responsible for any node-shape checks
 * (e.g. "is a [Node.Parent] with a [Node.Fillable] child that has an image fill").
 */
@Suppress("FunctionName")
fun NodeDiscoveryStrategy.Companion.TraverseDepthFirst(
    predicate: (node: Node, parent: Node.Parent?) -> Boolean,
): NodeDiscoveryStrategy = NodeDiscoveryStrategy { canvas ->
    buildList {
        canvas.traverseDepthFirst { node, parent ->
            if (predicate(node, parent)) add(node)
        }
    }
}
