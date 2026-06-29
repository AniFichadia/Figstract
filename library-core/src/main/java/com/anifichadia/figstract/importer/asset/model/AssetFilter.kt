package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node
import kotlinx.serialization.Serializable

@Serializable
data class AssetFilter(
    val canvasNameFilter: NodeFilter<Node.Canvas> = NodeFilter.empty(),
    val nodeNameFilter: NodeFilter<Node> = NodeFilter.empty(),
    val parentNameFilter: NodeFilter<Node.Parent> = NodeFilter.empty(),
) {
    companion object {
        val Empty = AssetFilter(
            canvasNameFilter = NodeFilter.empty(),
            nodeNameFilter = NodeFilter.empty(),
            parentNameFilter = NodeFilter.empty(),
        )
    }
}
