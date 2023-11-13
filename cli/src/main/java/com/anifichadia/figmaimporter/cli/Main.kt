package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.FigmaImporter
import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.anifichadia.figmaimporter.models.figma.FigmaApiImpl
import com.anifichadia.figmaimporter.models.figma.Node
import com.anifichadia.figmaimporter.models.figma.Node.Companion.traverseBreadthFirst
import com.anifichadia.figmaimporter.models.figma.Paint
import com.anifichadia.figmaimporter.models.importer.FigmaFileHandler
import com.anifichadia.figmaimporter.models.importer.Instruction
import com.anifichadia.figmaimporter.models.importer.exporting.ExportConfig
import com.anifichadia.figmaimporter.models.importer.importing.Destination
import com.anifichadia.figmaimporter.models.importer.importing.ImportPipelineStep.Companion.then
import com.anifichadia.figmaimporter.models.importer.importing.androidVectorColorToPlaceholder
import com.anifichadia.figmaimporter.models.importer.importing.svgToAvd
import com.anifichadia.figmaimporter.sanitise
import com.anifichadia.figmaimporter.to_snake_case
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.nio.file.Paths

suspend fun main(args: Array<String>) {
    //region Arg management
    val parser = ArgParser("Figma importer")

    //region args
    val authType by parser.option(ArgType.Choice<AuthType>(), fullName = "auth.type")
        .default(AuthType.AuthToken)
    val authToken by parser.option(ArgType.String, fullName = "auth")
        .required()

    val proxyHost by parser.option(ArgType.String, fullName = "proxy.host")
    val proxyPort by parser.option(ArgType.Int, fullName = "proxy.port")
    //endregion

    parser.parse(args)
    //endregion

    val authProvider = getAuth(authType, authToken)
    val proxy = getProxyConfig(proxyHost, proxyPort)

    val outDirectory = Paths.get("", "out").toAbsolutePath().toFile()

    //region Extractors
    val artworkDestination = Destination.directory(
        File(outDirectory, "artwork"),
    )
    val iconDestination = Destination.directory(
        File(outDirectory, "icons"),
    )

    val artworkFileHandler = FigmaFileHandler(
        // TODO: Set up value
        figmaFile = "",
    ) { response ->
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()

        canvases.map { canvas ->
            val canvasName = canvas.name
            buildList {
                canvas.traverseBreadthFirst { node, parent ->
                    if (parent != null && node is Node.Fillable && node.fills.any { it is Paint.Image }) {
                        val parentName = parent.name
                        add(
                            Instruction.of(
                                exportNodeId = parent.id,
                                exportConfig = ExportConfig.androidImageXxxHdpi,
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importDestination = artworkDestination,
                            )
                        )
                        add(
                            Instruction.of(
                                exportNodeId = node.id,
                                exportConfig = ExportConfig.androidImageXxxHdpi,
                                importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                    .sanitise()
                                    .to_snake_case(),
                                importDestination = artworkDestination,
                            )
                        )
                    }
                }
            }
        }.flatten()
    }

    val iconFileHandler = FigmaFileHandler(
        // TODO: Set up value
        figmaFile = "",
    ) { response ->
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()

        canvases.map { canvas ->
            buildList {
                canvas.traverseBreadthFirst { node, parent ->
                    if (parent != null && node is Node.Vector) {
                        val parentName = parent.name.let { it.split("/")[1] }
                        add(
                            Instruction.of(
                                exportNodeId = parent.id,
                                exportConfig = ExportConfig.svgIcon,
                                importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                importBefore = svgToAvd then androidVectorColorToPlaceholder,
                                importDestination = iconDestination,
                            )
                        )
                    }
                }
            }
        }.flatten()
    }
    //endregion

    val figmaHttpClient = HttpClientFactory.figma(proxy = proxy)
    val figmaApi = FigmaApiImpl(
        httpClient = figmaHttpClient,
        authProvider = authProvider,
    )
    val downloaderHttpClient = HttpClientFactory.downloader(proxy = proxy)
    val handlers = listOf(
        artworkFileHandler,
        iconFileHandler,
    )

    coroutineScope {
        FigmaImporter.importFromFigma(
            scope = this,
            figmaApi = figmaApi,
            downloaderHttpClient = downloaderHttpClient,
            handlers = handlers,
        )
    }
}

private fun getProxyConfig(proxyHost: String?, proxyPort: Int?): ProxyConfig? {
    return if (proxyHost != null) {
        ProxyBuilder.http(
            URLBuilder(
                protocol = URLProtocol.HTTP,
                host = proxyHost,
            )
                .apply {
                    if (proxyPort != null) {
                        this.port = proxyPort
                    }
                }
                .build()
        )
    } else {
        null
    }
}

enum class AuthType {
    AuthToken;
}

private fun getAuth(authType: AuthType, authToken: String): AuthProvider {
    when (authType) {
        AuthType.AuthToken -> return AuthProvider.AuthToken(authToken)
    }
}
