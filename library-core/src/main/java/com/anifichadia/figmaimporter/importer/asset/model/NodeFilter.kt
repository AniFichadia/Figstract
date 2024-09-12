package com.anifichadia.figmaimporter.importer.asset.model

import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.model.IncludeOrExcludeFilter

class NodeFilter<NodeT : Node>(
    override val include: Set<Regex>,
    override val exclude: Set<Regex>,
    override val getFilterableProperty: (NodeT) -> String = { it.name },
) : IncludeOrExcludeFilter<NodeT>()
