package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.FigmaImporterCommand

fun main(args: Array<String>) {
    FigmaImporterCommand(
        assetsCommand = RealAssetsCommand(),
        variablesCommand = RealVariablesCommand(),
    ).main(args)
}
