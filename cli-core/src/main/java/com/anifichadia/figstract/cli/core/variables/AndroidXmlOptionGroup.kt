package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.android.importer.variable.model.writer.AndroidXmlVariableDataWriter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum

class AndroidXmlOptionGroup private constructor() : OutputCodeOptionGroup("AndroidXml") {
    val splitByType by option(
        "--output${name}SplitByType",
        help = "Write each resource type (bools, integers, strings, colors, etc.) to its own file. Follows Android conventions. Default: true.",
    )
        .boolean()
        .default(true)

    val namespaceUsingCollectionName by option(
        "--output${name}NamespaceUsingCollectionName",
        help = "Namespace / prefix XML entries using the Figma collection name.",
    )
        .boolean()
        .default(true)

    val numberOutput by option(
        "--output${name}NumberOutput",
        help = "Controls how number variables are written. " +
            "NONE: skipped. " +
            "INTEGER: <integer> (fractional values truncated). " +
            "DIMEN: <dimen> with dp unit. " +
            "FLOAT: <item type=\"dimen\" format=\"float\"> (unitless). " +
            "Default: INTEGER.",
    )
        .enum<AndroidXmlVariableDataWriter.NumberOutput>()
        .default(AndroidXmlVariableDataWriter.NumberOutput.INTEGER)

    companion object {
        operator fun invoke() = AndroidXmlOptionGroup()
    }
}
