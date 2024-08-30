package com.anifichadia.figmaimporter.cli.core

import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.runBlocking
import java.io.File

abstract class AssetCommand : CliktCommand(name = "asset") {
    private val authProvider by requireObject<AuthProvider>()
    private val proxyConfig by findObject<ProxyConfig>()

    private val trackingEnabled: Boolean by option("--trackingEnabled")
        .boolean()
        .default(true)

    private val outPath: File by option("--out", "-o")
        .file(
            canBeFile = false,
            canBeDir = true,
        )
        .default(File("./out"))

    abstract val createHandlers: CliHelper.HandlerCreator

    override fun run() = runBlocking {
        CliHelper.execute(
            authProvider = authProvider,
            proxy = proxyConfig,
            trackingEnabled = trackingEnabled,
            outDirectory = outPath,
            createHandlers = createHandlers,
        )
    }
}
