package com.anifichadia.figmaimporter.cli.core.assets

import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.importer.asset.model.NodeFilter

data class AssetFilter(
    val canvasNameFilter: NodeFilter<Node.Canvas>,
    val nodeNameFilter: NodeFilter<Node>,
    val parentNameFilter: NodeFilter<Node.Parent>,
)
