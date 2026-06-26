package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.Conventions
import com.anifichadia.figstract.android.android
import com.anifichadia.figstract.cli.core.assets.handler.createArtworkFigmaFileHandler
import com.anifichadia.figstract.cli.core.assets.handler.createCustomFileHandler
import com.anifichadia.figstract.cli.core.assets.handler.createIconFigmaFileHandler
import com.anifichadia.figstract.cli.core.assets.model.AssetConfig
import com.anifichadia.figstract.cli.core.assets.model.BatchFormat
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.NodeTokenStringGenerator
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineDsl
import com.anifichadia.figstract.ios.ios
import com.anifichadia.figstract.type.serializer.RegexSerializer
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import java.io.File

class AssetBatchCommand : BaseAssetsCommand(
    name = "asset-batch",
) {
    private val batchConfig by option("--batchConfig")
        .help(
            """
            Path to a JSON batch config file. The file defines one or more asset batches to process.
            Comments are supported (// and /* */ style).
            
            Top-level structure:
            {
              "batches": [ ...AssetConfig... ]
            }
            
            Each batch is one of three types, identified by the "type" discriminator field:
            "Artwork", "Icon", or "Custom".
            
            ---
            
            Common fields (all types):
            
              "fileDefinition"  (required) Figma file to extract from:
                {
                  "fileKey":    string  (required) Figma file key
                  "branchName": string  (optional) branch name
                  "version":    string  (optional) file version
                }
            
              "enabled":          boolean  (required) set false to skip this batch
              "outDirectory":     string   (optional) overrides the global output directory for this batch
              "jsonPath":         string   (optional) JsonPath expression to locate nodes within the Figma file
              "instructionLimit": int      (optional) max number of assets to process
            
              "assetFilter": (optional) include/exclude assets by canvas, node, or parent name.
                Each filter accepts regex patterns:
                {
                  "canvasNameFilter": { "include": ["regex"], "exclude": ["regex"] },
                  "nodeNameFilter":   { "include": ["regex"], "exclude": ["regex"] },
                  "parentNameFilter": { "include": ["regex"], "exclude": ["regex"] }
                }
            
              "renamingMap": (optional) rename canvases or nodes before name generation:
                {
                  "canvases": { "OldCanvasName": "NewCanvasName" },
                  "nodes":    { "OldNodeName":   "NewNodeName"   }
                }
            
            ---
            
            Type: "Artwork"
            Extracts artwork images, scaling to density/scale buckets per platform.
            
              "namingFormats": (optional) token format strings for generated file names.
                {
                  "androidFormat": string  (default: "{canvas.name}_{node.name}")
                  "iosFormat":     string  (default: "{canvas.name}{node.name}")
                  "webFormat":     string  (default: "{canvas.name}_{node.name}")
                  "webCasing":     string  (optional) one of: LOWER_CAMEL_CASE, UPPER_CAMEL_CASE,
                                           LOWER_SNAKE_CASE, UPPER_SNAKE_CASE, LOWER_KEBAB_CASE,
                                           UPPER_KEBAB_CASE  (default: LOWER_SNAKE_CASE)
                }
            
              "platformOptions": (optional) enable/disable per-platform output:
                {
                  "androidEnabled": boolean  (default: true)
                  "iosEnabled":     boolean  (default: true)
                  "webEnabled":     boolean  (default: true)
                }
            
              "iosGroupByTokenNamingFormat": string  (optional) token format for iOS asset catalog grouping
            
              "createUncropped": boolean  (default: true)  export full uncropped image
              "createCropped":   boolean  (default: false) export cropped image
            
              "androidOutputDensityBuckets": array  (optional) subset of:
                ["LDPI", "MDPI", "HDPI", "XHDPI", "XXHDPI", "XXXHDPI"]  (default: all)
            
              "iosOutputScales":  array   (optional) subset of: ["1x", "2x", "3x"]  (default: all)
              "iosOutputFormat":  string  (optional) one of: Default, Heic, PngLossy  (default: Default)
            
            Example:
              {
                "type": "Artwork",
                "fileDefinition": { "fileKey": "abc123" },
                "enabled": true,
                "createCropped": true,
                "iosOutputFormat": "Heic",
                "platformOptions": { "webEnabled": false }
              }
            
            ---
            
            Type: "Icon"
            Extracts icons. Android output is converted to AVD; iOS output is stored in an asset catalog.
            
              "namingFormats": (optional) token format strings for generated file names.
                {
                  "androidFormat": string  (default: "ic_{node.name}")
                  "iosFormat":     string  (default: "{node.name}")
                  "webFormat":     string  (default: "{node.name}")
                  "webCasing":     string  (optional, see Artwork for values)
                }
            
              "platformOptions":             (optional, see Artwork)
              "iosGroupByTokenNamingFormat": string  (optional, see Artwork)
            
            Example:
              {
                "type": "Icon",
                "fileDefinition": { "fileKey": "abc123" },
                "enabled": true,
                "namingFormats": { "androidFormat": "ic_{node.name}", "iosFormat": "{node.name}", "webFormat": "{node.name}" },
                "platformOptions": { "iosEnabled": false }
              }
            
            ---
            
            Type: "Custom"
            Fully custom pipeline defined as a DSL string. Gives direct control over export and processing.
            
              "pipelineDefinition": string  (required) pipeline DSL steps, e.g.:
                "scale(scale=2.0) -> and(convertToWebPLossy(qualityPercent=75), convertToPngLossless())"
                See pipeline DSL documentation for available steps and syntax.
            
              "exportConfig": (required) how to request the asset from Figma:
                {
                  "format":            string   (required) one of: SVG, PNG, JPG, PDF
                  "scale":             float    (optional, 0.01–4.0, default: 1.0)
                  "contentsOnly":      boolean  (optional)
                  "useAbsoluteBounds": boolean  (optional)
                }
            
              "namingFormat": string  (required) token format string for generated file names
              "namingCasing": string  (required) one of: LOWER_CAMEL_CASE, UPPER_CAMEL_CASE,
                                       LOWER_SNAKE_CASE, UPPER_SNAKE_CASE, LOWER_KEBAB_CASE,
                                       UPPER_KEBAB_CASE
            
            Example:
              {
                "type": "Custom",
                "fileDefinition": { "fileKey": "abc123" },
                "enabled": true,
                "exportConfig": { "format": "PNG", "scale": 3.0 },
                "namingFormat": "{node.name}",
                "namingCasing": "LOWER_SNAKE_CASE",
                "pipelineDefinition": "convertToWebPLossy(qualityPercent=80) -> destination.directory(path=web/assets)"
              }
            """.trimIndent(),
        )
        .file(
            mustExist = true,
            canBeFile = true,
            canBeDir = false,
        )
        .convert { file ->
            try {
                json.decodeFromString<BatchFormat>(file.readText())
            } catch (e: Exception) {
                throw BadParameterValue("Failed to parse batch config file '$file': ${e.message}")
            }
        }
        .required()

    override fun createHandlers(outDirectory: File): List<AssetFileHandler> {
        return batchConfig.batches.filter { it.enabled }.map { assetConfig ->
            val outDirectory = assetConfig.outDirectory?.let(::File) ?: outDirectory

            when (assetConfig) {
                is AssetConfig.Convention -> {
                    //region Common options
                    val androidOutDirectory =
                        File(outDirectory, "android").takeIf { assetConfig.platformOptions.androidEnabled }
                    val iosOutDirectory = File(outDirectory, "ios").takeIf { assetConfig.platformOptions.iosEnabled }
                    val webOutDirectory = File(outDirectory, "web").takeIf { assetConfig.platformOptions.webEnabled }

                    val androidNameGenerator = NodeTokenStringGenerator(
                        format = assetConfig.namingFormats.androidFormat,
                        casing = Conventions.Casing.android,
                    )
                    val iosNameGenerator = NodeTokenStringGenerator(
                        format = assetConfig.namingFormats.iosFormat,
                        casing = Conventions.Casing.ios,
                    )
                    val webNameGenerator = NodeTokenStringGenerator(
                        format = assetConfig.namingFormats.webFormat,
                        casing = assetConfig.namingFormats.webCasing,
                    )

                    val iosGroupByToken = assetConfig.iosGroupByTokenNamingFormat?.let {
                        NodeTokenStringGenerator(
                            format = it,
                            casing = Conventions.Casing.ios,
                        )
                    }
                    //endregion

                    when (assetConfig) {
                        is AssetConfig.Artwork -> createArtworkFigmaFileHandler(
                            figmaFileDefinition = assetConfig.fileDefinition,
                            createUncropped = assetConfig.createUncropped,
                            createCropped = assetConfig.createCropped,
                            androidOutDirectory = androidOutDirectory,
                            iosOutDirectory = iosOutDirectory,
                            webOutDirectory = webOutDirectory,
                            assetFilter = assetConfig.assetFilter,
                            renamingMap = assetConfig.renamingMap,
                            androidNameGenerator = androidNameGenerator,
                            iosNameGenerator = iosNameGenerator,
                            webNameGenerator = webNameGenerator,
                            jsonPath = assetConfig.jsonPath,
                            androidOutputDensityBuckets = assetConfig.androidOutputDensityBuckets,
                            iosOutputScales = assetConfig.iosOutputScales,
                            iosOutputFormat = assetConfig.iosOutputFormat,
                            iosGroupByToken = iosGroupByToken,
                            instructionLimit = assetConfig.instructionLimit,
                        )

                        is AssetConfig.Icon -> createIconFigmaFileHandler(
                            figmaFileDefinition = assetConfig.fileDefinition,
                            androidOutDirectory = androidOutDirectory,
                            iosOutDirectory = iosOutDirectory,
                            webOutDirectory = webOutDirectory,
                            assetFilter = assetConfig.assetFilter,
                            renamingMap = assetConfig.renamingMap,
                            androidNameGenerator = androidNameGenerator,
                            iosNameGenerator = iosNameGenerator,
                            webNameGenerator = webNameGenerator,
                            jsonPath = assetConfig.jsonPath,
                            iosGroupByToken = iosGroupByToken,
                            instructionLimit = assetConfig.instructionLimit,
                        )
                    }
                }

                is AssetConfig.Custom -> {
                    val importPipelineDsl = ImportPipelineDsl(
                        registry = combinedRegistries(outDirectory),
                    )

                    val steps = importPipelineDsl.parse(assetConfig.pipelineDefinition)
                    val importPipeline = ImportPipeline(
                        steps = steps,
                    )

                    createCustomFileHandler(
                        figmaFileDefinition = assetConfig.fileDefinition,

                        importPipeline = importPipeline,

                        jsonPath = assetConfig.jsonPath,
                        exportConfig = assetConfig.exportConfig,

                        assetFilter = assetConfig.assetFilter,
                        renamingMap = assetConfig.renamingMap,
                        nameGenerator = NodeTokenStringGenerator(
                            format = assetConfig.namingFormat,
                            casing = assetConfig.namingCasing,
                        ),
                        instructionLimit = assetConfig.instructionLimit,
                    )
                }
            }
        }
    }

    companion object {
        private val json = Json {
            allowComments = true

            serializersModule = SerializersModule {
                contextual(RegexSerializer)

                polymorphic(AssetConfig::class) {
                    subclass(AssetConfig.Artwork::class)
                    subclass(AssetConfig.Icon::class)
                    subclass(AssetConfig.Custom::class)
                }
            }
        }
    }
}
