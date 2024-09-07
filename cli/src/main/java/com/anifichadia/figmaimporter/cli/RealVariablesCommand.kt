package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.VariablesCommand
import com.anifichadia.figmaimporter.importer.variable.model.VariableDataWriter
import com.anifichadia.figmaimporter.importer.variable.model.VariableFileHandler
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

class RealVariablesCommand : VariablesCommand() {
    private val figmaFiles by option("--figmaFile")
        .multiple()

    private val filters by VariableFilterOptionGroup()

    override fun createHandlers(outDirectory: File): List<VariableFileHandler> {
        val writers: List<VariableDataWriter> = buildList {
        }
        if (writers.isEmpty()) {
            throw BadParameterValue("No outputs have been defined")
        }

        return figmaFiles.map { figmaFile ->
            VariableFileHandler(
                figmaFile = figmaFile,
                filter = filters.toVariableFilter(),
                writers = writers,
            )
        }
    }
}
