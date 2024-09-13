package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.cli.core.outDirectory
import com.anifichadia.figstract.figma.api.FigmaApi
import com.anifichadia.figstract.importer.variable.FigmaVariableImporter
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.requireObject
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

    private val outDirectory by outDirectory()

    abstract fun createHandlers(outDirectory: File): List<VariableFileHandler>

    override fun run() = runBlocking {
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
