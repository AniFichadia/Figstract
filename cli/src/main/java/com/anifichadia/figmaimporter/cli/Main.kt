package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.cli.core.FigmaImporterCommand

fun main(args: Array<String>) {
    FigmaImporterCommand(
        assetCommand = RealAssetCommand(),
        variablesCommand = RealVariablesCommand(),
    ).main(args)
}
