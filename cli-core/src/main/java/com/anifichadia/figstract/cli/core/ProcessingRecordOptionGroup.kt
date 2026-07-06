package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean

class ProcessingRecordOptionGroup : OptionGroup() {
    val enabled by option(
        "--processingRecordEnabled",
        help = """
            |Processing records prevent re-processing Figma files when it has not been updated. This only takes into 
            |account a Figma file's last updated time and not the state of the output directory.
            |The processing record will be stored in the output directory for this command.
        """.trimMargin(),
    )
        .boolean()
        .default(true)

    val name by option(
        "--processingRecordName",
        help = """
            |A unique name suffix for the processing record file itself. This is useful when performing multiple runs
            |of the importer on the same Figma file but with different configurations to extract different assets.
            |This is distinct from a batch's own name (see asset-batch's --batchConfig), which disambiguates entries
            |within a single processing record file rather than the file itself.
        """.trimMargin(),
    )
}
