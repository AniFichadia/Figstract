package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.HttpClientFactory
import com.anifichadia.figstract.cli.core.assets.BaseAssetsCommand
import com.anifichadia.figstract.cli.core.variables.VariablesCommand
import com.anifichadia.figstract.figma.api.FigmaApiImpl
import com.anifichadia.figstract.figma.api.FigmaApiProxyWithFlowControl
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int

class FigstractCommand private constructor() : SuspendingCliktCommand(
    name = "figstract",
) {
    override val printHelpOnEmptyArgs = true

    private val auth by AuthOptionGroup()
    private val proxy by ProxyOptionGroup()
    private val figmaApiConcurrencyLimit by option("--figmaApiConcurrencyLimit")
        .int()
        .default(value = FigmaApiProxyWithFlowControl.DEFAULT_CONCURRENCY_LIMIT)
    private val figmaApiRetryLimit by option("--figmaApiRetryLimit")
        .int()
        .default(value = FigmaApiProxyWithFlowControl.DEFAULT_RETRY_LIMIT)

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
            FigmaApiProxyWithFlowControl(
                wrapped = FigmaApiImpl(
                    httpClient = figmaHttpClient,
                    authProvider = authProvider,
                ),
                concurrencyLimit = figmaApiConcurrencyLimit,
                retryLimit = figmaApiRetryLimit,
            )
        )
    }

    companion object {
        operator fun invoke(
            assetsCommand: BaseAssetsCommand,
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
