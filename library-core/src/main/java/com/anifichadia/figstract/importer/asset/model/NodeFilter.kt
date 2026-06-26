package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.model.IncludeOrExcludeFilter
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class NodeFilter<NodeT : Node>(
    override val include: Set<@Contextual Regex> = emptySet(),
    override val exclude: Set<@Contextual Regex> = emptySet(),
    @Transient
    override val getFilterableProperty: (NodeT) -> String = { it.name },
) : IncludeOrExcludeFilter<NodeT>() {
    companion object {
        fun <NodeT : Node> empty() = NodeFilter<NodeT>(
            include = emptySet(),
            exclude = emptySet(),
        )
    }
}
