package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.figma.api.FigmaApi
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int

class FigmaImporterCommand private constructor() : CliktCommand(
    name = "figstract",
    printHelpOnEmptyArgs = true,
) {
    private val authType: AuthType by option("--authType")
        .enum<AuthType>()
        .default(AuthType.AccessToken)
    private val authToken: String by option("--auth")
        .required()

    private val proxyHost: String? by option("--proxyHost")
    private val proxyPort: Int? by option("--proxyPort")
        .int()

    override fun run() {
        val authProvider = authType.createAuthProvider(authToken)
        currentContext.findOrSetObject { authProvider }

        val proxyConfig = getProxyConfig(proxyHost, proxyPort)
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
            assetCommand: AssetCommand,
            variablesCommand: VariablesCommand,
        ): FigmaImporterCommand {
            return FigmaImporterCommand()
                .subcommands(
                    assetCommand,
                    variablesCommand,
                )
        }
    }
}
