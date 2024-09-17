package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.HttpClientFactory
import com.anifichadia.figstract.cli.core.assets.AssetsCommand
import com.anifichadia.figstract.cli.core.variables.VariablesCommand
import com.anifichadia.figstract.figma.api.FigmaApi
import com.anifichadia.figstract.figma.api.FigmaApiImpl
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate

class FigstractCommand private constructor() : SuspendingCliktCommand(
    name = "figstract",
) {
    override val printHelpOnEmptyArgs = true

    private val auth by AuthOptionGroup()
    private val proxy by ProxyOptionGroup()

    override suspend fun run() {
        val authProvider = auth.authProvider
        currentContext.findOrSetObject { authProvider }

        val proxyConfig = try {
            proxy.proxyConfig
        } catch (e: IllegalArgumentException) {
            throw BadParameterValue(e.message ?: "Couldn't create proxy")
        }
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
        ): FigstractCommand {
            return FigstractCommand()
                .subcommands(
                    assetsCommand,
                    variablesCommand,
                )
        }
    }
}
