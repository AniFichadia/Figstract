package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.type.fold
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

fun CliktCommand.outDirectory(): OptionWithValues<File, File, File> {
    return option(
        "--out", "-o",
        help = """
            |Output directory for $commandName. The default output directory implicitly includes a subdirectory for 
            |$commandName. But if a value is supplied, the directory will be used without this implicit behaviour.
        """.trimMargin(),
    )
        .file(
            canBeFile = false,
            canBeDir = true,
        )
        .default(File("./out").fold(commandName))
}
