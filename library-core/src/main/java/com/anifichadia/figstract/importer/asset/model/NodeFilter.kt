package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.model.IncludeOrExcludeFilter

class NodeFilter<NodeT : Node>(
    override val include: Set<Regex>,
    override val exclude: Set<Regex>,
    override val getFilterableProperty: (NodeT) -> String = { it.name },
) : IncludeOrExcludeFilter<NodeT>()
