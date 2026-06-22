package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node

data class AssetFilter(
    val canvasNameFilter: NodeFilter<Node.Canvas>,
    val nodeNameFilter: NodeFilter<Node>,
    val parentNameFilter: NodeFilter<Node.Parent>,
)
