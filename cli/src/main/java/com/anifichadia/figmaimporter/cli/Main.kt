package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.FigmaImporterCommand
import com.anifichadia.figmaimporter.cli.core.assets.RealAssetsCommand
import com.anifichadia.figmaimporter.cli.core.variables.RealVariablesCommand

fun main(args: Array<String>) {
    FigmaImporterCommand(
        assetsCommand = RealAssetsCommand(),
        variablesCommand = RealVariablesCommand(),
    ).main(args)
}
