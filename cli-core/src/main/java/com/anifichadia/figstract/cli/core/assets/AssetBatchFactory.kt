package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.Conventions
import com.anifichadia.figstract.android.android
import com.anifichadia.figstract.cli.core.assets.handler.createArtworkFigmaFileHandler
import com.anifichadia.figstract.cli.core.assets.handler.createCustomFileHandler
import com.anifichadia.figstract.cli.core.assets.handler.createIconFigmaFileHandler
import com.anifichadia.figstract.cli.core.assets.model.AssetConfig
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.NodeTokenStringGenerator
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.importing.dsl.ImportPipelineDsl
import com.anifichadia.figstract.ios.ios
import java.io.File

fun createHandlersFromBatches(batches: List<AssetConfig>, outDirectory: File): List<AssetFileHandler> {
    return batches.filter { it.enabled }.map { assetConfig ->
        val outDirectory = assetConfig.outDirectory?.let(::File) ?: outDirectory

        when (assetConfig) {
            is AssetConfig.Convention -> {
                //region Common options
                val platformOptions = assetConfig.platformOptions
                val androidOutDirectory = platformOptions.androidDirectory(outDirectory)
                val iosOutDirectory = platformOptions.iosDirectory(outDirectory)
                val webOutDirectory = platformOptions.webDirectory(outDirectory)

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
