package com.anifichadia.figmaimporter.cli.core.variables

import com.anifichadia.figmaimporter.figma.api.FigmaApi
import com.anifichadia.figmaimporter.importer.variable.FigmaVariableImporter
import com.anifichadia.figmaimporter.importer.variable.model.VariableFileHandler
import com.anifichadia.figmaimporter.type.fold
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.sources.PropertiesValueSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File

abstract class VariablesCommand : CliktCommand(
    name = "variables",
    help = """
        Extracts variables from figma files, such as booleans, numbers, strings, and colors
    """.trimIndent(),
    printHelpOnEmptyArgs = true,
) {
    init {
        context {
            valueSources(
                PropertiesValueSource.from("$commandName.properties"),
            )
        }
    }

    private val figmaApi by requireObject<FigmaApi>()

    private val outPath: File by option("--out", "-o")
        .file(
            canBeFile = false,
            canBeDir = true,
        )
        .default(File("./out"))

    abstract fun createHandlers(outDirectory: File): List<VariableFileHandler>

    override fun run() = runBlocking {
        val outDirectory = outPath.fold("variables")

        val importer = FigmaVariableImporter(
            figmaApi = figmaApi,
        )

        coroutineScope {
            importer.importFromFigma(
                handlers = createHandlers(outDirectory),
            )
        }
    }
}
