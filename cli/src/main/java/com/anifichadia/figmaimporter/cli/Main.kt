package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.FigmaImporter
import com.anifichadia.figmaimporter.HttpClientFactory
import com.anifichadia.figmaimporter.android.figma.model.androidImageXxxHdpi
import com.anifichadia.figmaimporter.android.model.drawable.DensityBucket
import com.anifichadia.figmaimporter.android.model.importing.androidImageScaleAndStoreInDensityBuckets
import com.anifichadia.figmaimporter.android.model.importing.androidSvgToAvd
import com.anifichadia.figmaimporter.android.model.importing.androidVectorColorToPlaceholder
import com.anifichadia.figmaimporter.figma.api.FigmaApiImpl
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.figma.model.Node
import com.anifichadia.figmaimporter.figma.model.Node.Companion.traverseBreadthFirst
import com.anifichadia.figmaimporter.figma.model.Paint
import com.anifichadia.figmaimporter.ios.figma.model.ios3xImage
import com.anifichadia.figmaimporter.ios.figma.model.iosIcon
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Scale
import com.anifichadia.figmaimporter.ios.model.assetcatalog.Type
import com.anifichadia.figmaimporter.ios.model.assetcatalog.createAssetCatalogContentDirectory
import com.anifichadia.figmaimporter.ios.model.assetcatalog.createAssetCatalogRootDirectory
import com.anifichadia.figmaimporter.ios.model.importing.assetCatalogFinalisationLifecycle
import com.anifichadia.figmaimporter.ios.model.importing.iosScaleAndStoreInAssetCatalog
import com.anifichadia.figmaimporter.ios.model.importing.iosStoreInAssetCatalog
import com.anifichadia.figmaimporter.model.FigmaFileHandler
import com.anifichadia.figmaimporter.model.Instruction.Companion.addInstruction
import com.anifichadia.figmaimporter.model.Instruction.Companion.buildInstructions
import com.anifichadia.figmaimporter.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.model.exporting.svg
import com.anifichadia.figmaimporter.model.importing.Destination
import com.anifichadia.figmaimporter.model.importing.Destination.Companion.directoryDestination
import com.anifichadia.figmaimporter.model.importing.ImportPipeline
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.then
import com.anifichadia.figmaimporter.model.tracking.JsonFileProcessingRecordRepository
import com.anifichadia.figmaimporter.model.tracking.NoOpProcessingRecordRepository
import com.anifichadia.figmaimporter.type.fold
import com.anifichadia.figmaimporter.util.FileManagement
import com.anifichadia.figmaimporter.util.ToUpperCamelCase
import com.anifichadia.figmaimporter.util.createLogger
import com.anifichadia.figmaimporter.util.sanitise
import com.anifichadia.figmaimporter.util.to_snake_case
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

    val authProvider = authType.createAuthProvider(authToken)
    val proxy = getProxyConfig(proxyHost, proxyPort)

    val outDirectory = FileManagement.outDirectory(outPath)

    val androidEnabled = true
    val iosEnabled = true
    val webEnabled = true

    val artworkEnabled = true
    val iconsEnabled = true

    // This is for testing. Providing a non-null value will run a take operation on the list of all instructions for each handler
    @Suppress("RedundantNullableReturnType")
    val instructionLimit: Int? = null

    //region Extractors
    val androidOutDirectory = File(outDirectory, "android")
    val iosOutDirectory = File(outDirectory, "ios")
    val webOutDirectory = File(outDirectory, "web")

    val artworkFileHandler = createArtworkFigmaFileHandler(
        enabled = artworkEnabled,
        androidOutDirectory = androidOutDirectory,
        iosOutDirectory = iosOutDirectory,
        webOutDirectory = webOutDirectory,
        androidEnabled = androidEnabled,
        iosEnabled = iosEnabled,
        webEnabled = webEnabled,
        instructionLimit = instructionLimit,
    )

    val iconFileHandler = createIconFigmaFileHandler(
        enabled = iconsEnabled,
        androidOutDirectory = androidOutDirectory,
        iosOutDirectory = iosOutDirectory,
        webOutDirectory = webOutDirectory,
        androidEnabled = androidEnabled,
        iosEnabled = iosEnabled,
        webEnabled = webEnabled,
        instructionLimit = instructionLimit,
    )
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
                artworkFileHandler,
                iconFileHandler,
            ),
        )
    }
}

val timingLogger = createLogger("Timing")

@Suppress("SameParameterValue")
private fun createArtworkFigmaFileHandler(
    enabled: Boolean,
    androidOutDirectory: File,
    iosOutDirectory: File,
    webOutDirectory: File,
    androidEnabled: Boolean,
    iosEnabled: Boolean,
    webEnabled: Boolean,
    instructionLimit: Int?,
): FigmaFileHandler? {
    if (!enabled) return null

    val androidOutputDirectory = File(androidOutDirectory, "artwork")
    val androidImportPipeline = ImportPipeline(
        steps = androidImageScaleAndStoreInDensityBuckets(androidOutputDirectory, DensityBucket.XXXHDPI),
        // Destination is handled by the steps
        destination = Destination.None,
    )

    val iosOutputDirectory = File(iosOutDirectory, "artwork")
    val iosAssetCatalogRootDirectory = createAssetCatalogRootDirectory(iosOutputDirectory)
    val iosContentDirectory = createAssetCatalogContentDirectory(iosAssetCatalogRootDirectory, "Images")
    val iosImportPipeline = ImportPipeline(
        steps = iosScaleAndStoreInAssetCatalog(iosContentDirectory, Type.IMAGE_SET, Scale.`3x`),
        // Destination is handled by the steps
        destination = Destination.None,
    )
    val iosAssetCatalogLifecycle = if (iosEnabled) {
        assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory)
    } else {
        FigmaFileHandler.Lifecycle.NoOp
    }

    val webOutputDirectory = File(webOutDirectory, "artwork")
    val webImportPipeline = ImportPipeline(
        destination = directoryDestination(webOutputDirectory),
    )

    val timingLifecycle = FigmaFileHandler.Lifecycle.Timing()
    val timingLoggingLifecycle = object : FigmaFileHandler.Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Artwork retrieval timing: \n$timingLifecycle" }
        }
    }

    val artworkFileHandler = FigmaFileHandler(
        // TODO: Set up value
        figmaFile = "",
        lifecycle = FigmaFileHandler.Lifecycle.Combined(
            iosAssetCatalogLifecycle,
            timingLifecycle,
            timingLoggingLifecycle,
        ),
    ) { response, _ ->
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()

        canvases.map { canvas ->
            val canvasName = canvas.name
            buildInstructions {
                canvas.traverseBreadthFirst { node, parent ->
                    if (parent != null && node is Node.Fillable && node.fills.any { it is Paint.Image }) {
                        val parentName = parent.name

                        if (androidEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = androidImageXxxHdpi,
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = androidImportPipeline,
                            )
                            addInstruction(
                                exportNodeId = node.id,
                                exportConfig = androidImageXxxHdpi,
                                importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = androidImportPipeline,
                            )
                        }

                        if (iosEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = ios3xImage,
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = iosImportPipeline,
                            )
                            addInstruction(
                                exportNodeId = node.id,
                                exportConfig = ios3xImage,
                                importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = iosImportPipeline,
                            )
                        }

                        if (webEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                importOutputName = "artwork_${canvasName}_${parentName}"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = webImportPipeline,
                            )
                            addInstruction(
                                exportNodeId = node.id,
                                exportConfig = ExportConfig(ExportSetting.Format.PNG),
                                importOutputName = "artwork_${canvasName}_${parentName}_cropped"
                                    .sanitise()
                                    .to_snake_case(),
                                importPipeline = webImportPipeline,
                            )
                        }
                    }
                }
            }
        }.flatten()
            .let {
                if (instructionLimit != null) {
                    it.take(instructionLimit)
                } else {
                    it
                }
            }
    }

    return artworkFileHandler
}

@Suppress("SameParameterValue")
private fun createIconFigmaFileHandler(
    enabled: Boolean,
    androidOutDirectory: File,
    iosOutDirectory: File,
    webOutDirectory: File,
    androidEnabled: Boolean,
    iosEnabled: Boolean,
    webEnabled: Boolean,
    instructionLimit: Int?,
): FigmaFileHandler? {
    if (!enabled) return null

    val androidOutputDirectory = androidOutDirectory.fold("icons", "drawable")
    val androidImportPipeline = ImportPipeline(
        steps = androidSvgToAvd then androidVectorColorToPlaceholder,
        destination = directoryDestination(androidOutputDirectory),
    )

    val iosDirectory = File(iosOutDirectory, "icons")
    val iosAssetCatalogRootDirectory = createAssetCatalogRootDirectory(iosDirectory)
    val iosContentDirectory = createAssetCatalogContentDirectory(iosAssetCatalogRootDirectory, "Images")
    val iosImportPipeline = ImportPipeline(
        // TODO right config?
        steps = iosStoreInAssetCatalog(iosContentDirectory, Type.ICON_SET, Scale.`1x`),
        // Destination is handled by steps
        destination = Destination.None,
    )
    val iosAssetCatalogLifecycle = if (iosEnabled) {
        assetCatalogFinalisationLifecycle(iosAssetCatalogRootDirectory)
    } else {
        FigmaFileHandler.Lifecycle.NoOp
    }

    val webOutputDirectory = File(webOutDirectory, "icons")
    val webImportPipeline = ImportPipeline(
        destination = directoryDestination(webOutputDirectory),
    )

    val timingLifecycle = FigmaFileHandler.Lifecycle.Timing()
    val timingLoggingLifecycle = object : FigmaFileHandler.Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Icon retrieval timing: \n$timingLifecycle" }
        }
    }

    val iconFileHandler = FigmaFileHandler(
        // TODO: Set up value
        figmaFile = "",
        // Icons are smaller, so we can retrieve more at the same time
        assetsPerChunk = 25,
        lifecycle = FigmaFileHandler.Lifecycle.Combined(
            iosAssetCatalogLifecycle,
            timingLifecycle,
            timingLoggingLifecycle,
        ),
    ) { response, _ ->
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()

        canvases.map { canvas ->
            buildInstructions {
                canvas.traverseBreadthFirst { node, parent ->
                    if (parent != null && node is Node.Vector) {
                        val parentName = parent.name.let { it.split("/")[1] }

                        if (androidEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = svg,
                                importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                importPipeline = androidImportPipeline,
                            )
                        }

                        if (iosEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = iosIcon,
                                importOutputName = parentName.sanitise().ToUpperCamelCase(),
                                importPipeline = iosImportPipeline,
                            )
                        }

                        if (webEnabled) {
                            addInstruction(
                                exportNodeId = parent.id,
                                exportConfig = svg,
                                importOutputName = "ic_${parentName}".sanitise().to_snake_case(),
                                importPipeline = webImportPipeline,
                            )
                        }
                    }
                }
            }
        }.flatten()
            .let {
                if (instructionLimit != null) {
                    it.take(instructionLimit)
                } else {
                    it
                }
            }
    }

    return iconFileHandler
}
