package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.importer.asset.model.NodeFilter

data class AssetFilter(
    val canvasNameFilter: NodeFilter<Node.Canvas>,
    val nodeNameFilter: NodeFilter<Node>,
    val parentNameFilter: NodeFilter<Node.Parent>,
)
