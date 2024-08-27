package com.anifichadia.figmaimporter.cli.core

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import java.io.File

abstract class DefaultFigmaImporterCommand() : CliktCommand() {
    private val authType: AuthType by option("--authType")
        .enum<AuthType>()
        .default(AuthType.AccessToken)
    private val authToken: String by option("--auth")
        .required()

    private val proxyHost: String? by option("--proxyHost")
    private val proxyPort: Int? by option("--proxyPort")
        .int()

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
            authType = authType,
            authToken = authToken,
            proxyHost = proxyHost,
            proxyPort = proxyPort,
            trackingEnabled = trackingEnabled,
            outDirectory = outPath,
            createHandlers = createHandlers,
        )
    }
}
