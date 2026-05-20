package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.util.ToUpperCamelCase
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean

open class OutputCodeOptionGroup protected constructor(
    name: String,
) : OptionGroup() {
    protected val name = name.ToUpperCamelCase()

    val enabled by option("--output${this.name}")
        .boolean()
        .default(false)

    companion object {
        operator fun invoke(
            name: String,
        ) = OutputCodeOptionGroup(name)
    }
}

open class OutputCodeWithGroupingOptionGroup private constructor(
    name: String,
    logicalGroupingName: String,
) : OutputCodeOptionGroup(
    name = name,
) {
    private val logicalGroupingName = logicalGroupingName.ToUpperCamelCase()

    val logicalGrouping by option("--output${this.name}${this.logicalGroupingName}")
        .required()

    companion object {
        operator fun invoke(
            name: String,
            logicalGroupingName: String,
        ) = OutputCodeWithGroupingOptionGroup(name, logicalGroupingName).cooccurring()
    }
}
