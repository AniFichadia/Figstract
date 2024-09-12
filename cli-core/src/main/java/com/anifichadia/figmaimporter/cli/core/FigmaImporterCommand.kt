package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.cli.core.assets.AssetsCommand
import com.anifichadia.figmaimporter.cli.core.variables.VariablesCommand
import com.anifichadia.figmaimporter.figma.api.FigmaApi
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate

class FigmaImporterCommand private constructor() : CliktCommand(
    name = "figstract",
    printHelpOnEmptyArgs = true,
) {
    private val auth by AuthOptionGroup()
    private val proxy by ProxyOptionGroup()

    override fun run() {
        val authProvider = auth.authProvider
        currentContext.findOrSetObject { authProvider }

        val proxyConfig = proxy.proxyConfig
        if (proxyConfig != null) {
            currentContext.findOrSetObject { proxyConfig }
        }

        val figmaHttpClient = HttpClientFactory.figma(
            proxy = proxyConfig,
        )
        currentContext.findOrSetObject<FigmaApi> {
            FigmaApiImpl(
                httpClient = figmaHttpClient,
                authProvider = authProvider,
            )
        }
    }

    companion object {
        operator fun invoke(
            assetsCommand: AssetsCommand,
            variablesCommand: VariablesCommand,
        ): FigmaImporterCommand {
            return FigmaImporterCommand()
                .subcommands(
                    assetsCommand,
                    variablesCommand,
                )
        }
    }
}
