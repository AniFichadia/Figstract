package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node

/**
 * Discovers which [Node]s within a [Node.Canvas] should be considered for asset export.
 */
fun interface NodeDiscoveryStrategy {
    fun discover(canvas: Node.Canvas): List<Node>

    companion object
}
