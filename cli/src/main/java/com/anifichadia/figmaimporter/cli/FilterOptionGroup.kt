package com.anifichadia.figmaimporter.cli

import com.github.ajalt.clikt.core.MutuallyExclusiveGroupException
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option

class FilterOptionGroup(private val prefix: String) : OptionGroup() {
    private val includedCanvases by option("--${prefix}FilterIncludedCanvas")
        .multiple()
    private val excludedCanvases by option("--${prefix}FilterExcludedCanvas")
        .multiple()

    private val includedNodes by option("--${prefix}FilterIncludedNode")
        .multiple()
    private val excludedNodes by option("--${prefix}FilterExcludedNode")
        .multiple()

    private val includedParentNodes by option("--${prefix}FilterIncludedParentNode")
        .multiple()
    private val excludedParentNodes by option("--${prefix}FilterExcludedParentNode")
        .multiple()

    fun toAssetFilter(): AssetFilter {
        if (includedCanvases.isNotEmpty() && excludedCanvases.isNotEmpty()) {
            throw MutuallyExclusiveGroupException(
                listOf("${prefix}FilterIncludedCanvas", "${prefix}FilterExcludedCanvas"),
            )
        }
        if (includedNodes.isNotEmpty() && excludedNodes.isNotEmpty()) {
            throw MutuallyExclusiveGroupException(
                listOf("${prefix}FilterIncludedNode", "${prefix}FilterExcludedNode"),
            )
        }
        if (includedParentNodes.isNotEmpty() && excludedParentNodes.isNotEmpty()) {
            throw MutuallyExclusiveGroupException(
                listOf("${prefix}FilterIncludedParentNode", "${prefix}FilterExcludedParentNode"),
            )
        }

        return AssetFilter(
            include = AssetFilter.Filter.Include(
                canvasNames = includedCanvases,
                nodeNames = includedNodes,
                parentNodeNames = includedParentNodes,
            ),
            exclude = AssetFilter.Filter.Exclude(
                canvasNames = excludedCanvases,
                nodeNames = excludedNodes,
                parentNodeNames = excludedParentNodes,
            ),
        )
    }
}
