package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.cli.core.defaultJsonValueSource
import com.anifichadia.figstract.cli.core.defaultPropertyValueSource
import com.anifichadia.figstract.cli.core.getFigmaApi
import com.anifichadia.figstract.cli.core.outDirectory
import com.anifichadia.figstract.importer.variable.FigmaVariableImporter
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import kotlinx.coroutines.coroutineScope
import java.io.File

abstract class VariablesCommand : SuspendingCliktCommand(
    name = "variables",
) {
    override val printHelpOnEmptyArgs = true

    init {
        context {
            valueSources(
                defaultPropertyValueSource(),
                defaultJsonValueSource(),
            )
        }
    }

    private val figmaApi by getFigmaApi()

    private val outDirectory by outDirectory()

    override fun help(context: Context): String {
        return """
        Extracts variables from figma files, such as booleans, numbers, strings, and colors
    """.trimIndent()
    }

    abstract fun createHandlers(outDirectory: File): List<VariableFileHandler>

    override suspend fun run() {
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
