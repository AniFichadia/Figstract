package com.anifichadia.figmaimporter.cli.core.variables

import com.anifichadia.figmaimporter.util.ToUpperCamelCase
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean

class OutputCodeOptionGroup private constructor(
    name: String,
    logicalGroupingName: String,
) : OptionGroup() {
    private val name = name.ToUpperCamelCase()
    private val logicalGroupingName = logicalGroupingName.ToUpperCamelCase()

    val enabled by option("--output${this.name}")
        .boolean()
        .default(false)
    val logicalGrouping by option("--output${this.name}${this.logicalGroupingName}")
        .required()

    companion object {
        operator fun invoke(
            name: String,
            logicalGroupingName: String = "PackageName",
        ) = OutputCodeOptionGroup(name, logicalGroupingName).cooccurring()
    }
}
