package com.anifichadia.figstract.cli

import com.anifichadia.figstract.cli.core.FigmaImporterCommand
import com.anifichadia.figstract.cli.core.assets.RealAssetsCommand
import com.anifichadia.figstract.cli.core.variables.RealVariablesCommand

fun main(args: Array<String>) {
    FigmaImporterCommand(
        assetsCommand = RealAssetsCommand(),
        variablesCommand = RealVariablesCommand(),
    ).main(args)
}
