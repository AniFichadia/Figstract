package com.anifichadia.figstract.cli.core.variables

import com.anifichadia.figstract.ExperimentalFigstractApi
import com.anifichadia.figstract.android.importer.variable.model.AndroidComposeVariableDataWriter
import com.anifichadia.figstract.importer.variable.model.JsonVariableDataWriter
import com.anifichadia.figstract.importer.variable.model.VariableDataWriter
import com.anifichadia.figstract.importer.variable.model.VariableFileHandler
import com.anifichadia.figstract.ios.importer.variable.model.IosSwiftUiVariableDataWriter
import com.anifichadia.figstract.type.fold
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
    private val outputAndroidCompose by OutputCodeOptionGroup("AndroidCompose", "PackageName")
    private val outputIosSwiftUi by OutputCodeOptionGroup("IosSwiftUi", "Module")
    private val outputColorAsHex by option("--outputColorAsHex")
        .boolean()
        .default(true)

    override fun createHandlers(outDirectory: File): List<VariableFileHandler> {
        val writers = createWriters(outDirectory)
        if (writers.isEmpty()) throw BadParameterValue("No outputs have been defined")

        return figmaFiles.map { figmaFile ->
            VariableFileHandler(
                figmaFile = figmaFile,
                filter = filters.toVariableFilter(),
                writers = writers,
            )
        }
    }

    @OptIn(ExperimentalFigstractApi::class)
    private fun createWriters(outDirectory: File): List<VariableDataWriter> = buildList {
        if (outputJson) {
            add(
                JsonVariableDataWriter(
                    outDirectory = outDirectory.fold("json"),
                    colorAsHex = outputColorAsHex,
                )
            )
        }
        addIfEnabled(outputAndroidCompose) {
            AndroidComposeVariableDataWriter(
                outDirectory = outDirectory.fold("android", "compose"),
                packageName = it.logicalGrouping,
                colorAsHex = outputColorAsHex,
            )
        }
        addIfEnabled(outputIosSwiftUi) {
            println("Warning: iOS Swift UI variable output is experimental and is subject to change")

            IosSwiftUiVariableDataWriter(
                outDirectory = outDirectory.fold("ios", "swiftui"),
                modulePath = it.logicalGrouping,
            )
        }
    }

    private fun MutableList<VariableDataWriter>.addIfEnabled(
        option: OutputCodeOptionGroup?,
        create: (OutputCodeOptionGroup) -> VariableDataWriter,
    ) {
        if (option == null || !option.enabled) return

        add(create(option))
    }
}
