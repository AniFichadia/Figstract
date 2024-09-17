package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean

fun ParameterHolder.processingRecordEnabled(): OptionWithValues<Boolean, Boolean, Boolean> {
    return option(
        "--processingRecordEnabled",
        help = """
            |Processing records prevent re-processing Figma files when it has not been updated. This only takes into 
            |account a Figma file's last updated time and not the state of the output directory.
            |The processing record will be stored in the output directory for this command.
        """.trimMargin(),
    )
        .boolean()
        .default(true)
}
