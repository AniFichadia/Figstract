package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.HttpClientFactory
import com.anifichadia.figstract.cli.core.assets.AssetsCommand
import com.anifichadia.figstract.cli.core.variables.VariablesCommand
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

    private val logLevel by logLevel()

    override suspend fun run() {
        getRootLogger().level = logLevel.toLogbackLogLevel()

        val authProvider = auth.authProvider
        setAuthProvider(authProvider)

        val proxyConfig = try {
            proxy.proxyConfig
        } catch (e: IllegalArgumentException) {
            throw BadParameterValue(e.message ?: "Couldn't create proxy")
        }
        setProxy(proxyConfig)

        val figmaHttpClient = HttpClientFactory.figma(
            proxy = proxyConfig,
        )
        setFigmaApi(
            FigmaApiImpl(
                httpClient = figmaHttpClient,
                authProvider = authProvider,
            )
        )
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
