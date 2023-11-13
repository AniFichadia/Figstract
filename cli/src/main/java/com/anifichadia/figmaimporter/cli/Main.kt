package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.FigmaImporter
import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figmaimporter.android.model.drawable.DensityBucket
import com.anifichadia.figmaimporter.android.model.importing.androidImageDownscaleToBuckets
import com.anifichadia.figmaimporter.android.model.importing.androidSvgToAvd
import com.anifichadia.figmaimporter.android.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.anifichadia.figmaimporter.figma.api.AccessToken
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figmaimporter.figma.model.Paint
import com.anifichadia.figmaimporter.ios.figma.model.ios3xImage
import com.anifichadia.figmaimporter.ios.figma.model.iosIcon
import com.anifichadia.figmaimporter.ios.model.importing.ios3xDownscaleAndStoreInImageAssetCatalog
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.Instruction
import com.anifichadia.figmaimporter.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.model.exporting.svg
import com.anifichadia.figmaimporter.model.importing.Destination
import com.anifichadia.figmaimporter.model.importing.Destination.Companion.directoryDestination
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
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
        .default(AuthType.AccessToken)
    val authToken by parser.option(ArgType.String, fullName = "auth")
        .required()

    val proxyHost by parser.option(ArgType.String, fullName = "proxy.host")
    val proxyPort by parser.option(ArgType.Int, fullName = "proxy.port")

    val trackingDisabled by parser.option(ArgType.Boolean, fullName = "tracking.disabled")
        .default(false)

    val outPath by parser.option(ArgType.String, fullName = "out", shortName = "o")
        .default("out")
    //endregion

    parser.parse(args)
    //endregion

    val authProvider = getAuth(authType, authToken)
    val proxy = getProxyConfig(proxyHost, proxyPort)

    val outDirectory = Paths.get("", outPath).toAbsolutePath().toFile()

    val androidEnabled = true
    val iosEnabled = true
    val webEnabled = true

    val artworkEnabled = true
    val iconsEnabled = true

    //region Extractors
    val androidOutDirectory = File(outDirectory, "android")
    val iosOutDirectory = File(outDirectory, "ios")
    val webOutDirectory = File(outDirectory, "web")

    //region Artwork
    val androidArtworkDirectory = File(androidOutDirectory, "artwork")
    val androidArtworkPipeline = androidImageDownscaleToBuckets(androidArtworkDirectory, DensityBucket.XXXHDPI)
    // Destination is handled by androidArtworkPipeline
    val androidArtworkDestination = Destination.None

    val iosArtworkDirectory = File(iosOutDirectory, "artwork")
    val iosArtworkPipeline = ios3xDownscaleAndStoreInImageAssetCatalog(iosArtworkDirectory)
    // Destination is handled by iosArtworkPipeline
    val iosArtworkDestination = Destination.None

    val webArtworkDestination = directoryDestination(File(webOutDirectory, "artwork"))

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

                        if (androidEnabled) {
                            add(
                                Instruction.of(
                                    exportNodeId = parent.id,
                                    exportConfig = androidImageXxxHdpi,
                                    importOutputName = "artwork_${canvasName}_${parentName}"
                                        .sanitise()
                                        .to_snake_case(),
                                    importBefore = androidArtworkPipeline,
                                    importDestination = androidArtworkDestination,
                                )
                            )
                            add(
                                Instruction.of(
                                    exportNodeId = node.id,
                                    exportConfig = androidImageXxxHdpi,
                                    importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                        .sanitise()
                                        .to_snake_case(),
                                    importBefore = androidArtworkPipeline,
                                    importDestination = androidArtworkDestination,
                                )
                            )
                        }

                        if (iosEnabled) {
                            add(
                                Instruction.of(
                                    exportNodeId = parent.id,
                                    exportConfig = ios3xImage,
                                    importOutputName = "artwork_${canvasName}_${parentName}"
                                        .sanitise()
                                        .to_snake_case(),
                                    importBefore = iosArtworkPipeline,
                                    importDestination = iosArtworkDestination,
                                )
                            )
                            add(
                                Instruction.of(
                                    exportNodeId = node.id,
                                    exportConfig = ios3xImage,
                                    importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                        .sanitise()
                                        .to_snake_case(),
                                    importBefore = iosArtworkPipeline,
                                    importDestination = iosArtworkDestination,
                                )
                            )
                        }

                        if (webEnabled) {
                            add(
                                Instruction.of(
                                    exportNodeId = parent.id,
                                    exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                    importOutputName = "artwork_${canvasName}_${parentName}"
                                        .sanitise()
                                        .to_snake_case(),
                                    importDestination = webArtworkDestination,
                                )
                            )
                            add(
                                Instruction.of(
                                    exportNodeId = node.id,
                                    exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                    importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                        .sanitise()
                                        .to_snake_case(),
                                    importDestination = webArtworkDestination,
                                )
                            )
                        }
                    }
                }
            }
        }.flatten()
    }
    //endregion

    //region Icons
    val androidIconDestination = directoryDestination(File(androidOutDirectory, "icons"))
    val iosIconDestination = directoryDestination(File(iosOutDirectory, "icons"))
    val webIconDestination = directoryDestination(File(webOutDirectory, "icons"))
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

                        if (androidEnabled) {
                            add(
                                Instruction.of(
                                    exportNodeId = parent.id,
                                    exportConfig = svg,
                                    importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                    importBefore = androidSvgToAvd then androidVectorColorToPlaceholder,
                                    importDestination = androidIconDestination,
                                )
                            )
                        }

                        if (iosEnabled) {
                            add(
                                Instruction.of(
                                    exportNodeId = parent.id,
                                    exportConfig = iosIcon,
                                    importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                    importDestination = iosIconDestination,
                                )
                            )
                        }

                        if (webEnabled) {
                            add(
                                Instruction.of(
                                    exportNodeId = parent.id,
                                    exportConfig = svg,
                                    importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                    importDestination = webIconDestination,
                                )
                            )
                        }
                    }
                }
            }
        }.flatten()
    }
    //endregion
    //endregion

    val figmaHttpClient = HttpClientFactory.figma(proxy = proxy)
    val figmaApi = FigmaApiImpl(
        httpClient = figmaHttpClient,
        authProvider = authProvider,
    )
    val downloaderHttpClient = HttpClientFactory.downloader(proxy = proxy)
    val processingRecordRepository = if (!trackingDisabled) {
        JsonFileProcessingRecordRepository(
            recordFile = File(outDirectory, "processing_record.json")
        )
    } else {
        NoOpProcessingRecordRepository
    }

    val importer = FigmaImporter(
        figmaApi = figmaApi,
        downloaderHttpClient = downloaderHttpClient,
        processingRecordRepository = processingRecordRepository,
    )
    coroutineScope {
        importer.importFromFigma(
            handlers = listOfNotNull(
                artworkFileHandler.takeIf { artworkEnabled },
                iconFileHandler.takeIf { iconsEnabled },
            ),
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
    AccessToken,
    ;
}

private fun getAuth(authType: AuthType, authToken: String): AuthProvider {
    when (authType) {
        AuthType.AccessToken -> return AccessToken(authToken)
    }
}
