package com.anifichadia.figmaimporter.cli.core.variables

import com.anifichadia.figmaimporter.android.importer.variable.model.AndroidComposeVariableDataWriter
import com.anifichadia.figmaimporter.importer.variable.model.JsonVariableDataWriter
import com.anifichadia.figmaimporter.importer.variable.model.VariableDataWriter
import com.anifichadia.figmaimporter.importer.variable.model.VariableFileHandler
import com.anifichadia.figmaimporter.type.fold
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import java.io.File

class RealVariablesCommand : VariablesCommand() {
    private val figmaFiles by option("--figmaFile")
        .multiple()

    private val filters by VariableFilterOptionGroup()

    private val outputJson by option("--outputJson")
        .boolean()
        .default(false)
    private val outputAndroidCompose by OutputCodeOptionGroup("AndroidCompose")

    override fun createHandlers(outDirectory: File): List<VariableFileHandler> {
        val writers: List<VariableDataWriter> = buildList {
            if (outputJson) {
                add(
                    JsonVariableDataWriter(
                        outDirectory = outDirectory.fold("json"),
                    )
                )
            }
            outputAndroidCompose?.let {
                if (it.enabled) {
                    add(
                        AndroidComposeVariableDataWriter(
                            outDirectory = outDirectory.fold("android", "compose"),
                            packageName = it.logicalGrouping,
                        )
                    )
                }
            }
        }
        if (writers.isEmpty()) throw BadParameterValue("No outputs have been defined")

        return figmaFiles.map { figmaFile ->
            VariableFileHandler(
                figmaFile = figmaFile,
                filter = filters.toVariableFilter(),
                writers = writers,
            )
        }
    }
}
