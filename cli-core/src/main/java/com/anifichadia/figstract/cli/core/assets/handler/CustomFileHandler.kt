package com.anifichadia.figstract.cli.core.assets.handler

import com.anifichadia.figstract.cli.core.timingLogger
import com.anifichadia.figstract.figma.FigmaFileDefinition
import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.importer.asset.model.AssetFileHandler
import com.anifichadia.figstract.importer.asset.model.AssetFilter
import com.anifichadia.figstract.importer.asset.model.AssetRenamingMap
import com.anifichadia.figstract.importer.asset.model.FigmaAssetFileHandler
import com.anifichadia.figstract.importer.asset.model.Instruction
import com.anifichadia.figstract.importer.asset.model.JsonPath
import com.anifichadia.figstract.importer.asset.model.NodeDiscoveryStrategy
import com.anifichadia.figstract.importer.asset.model.NodeTokenStringGenerator
import com.anifichadia.figstract.importer.asset.model.asSeenNameTracker
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline
import com.anifichadia.figstract.importer.asset.model.toNamingContext

internal fun createCustomFileHandler(
    figmaFileDefinition: FigmaFileDefinition,

    importPipeline: ImportPipeline,

    jsonPath: String,
    exportConfig: ExportConfig,

    assetFilter: AssetFilter,
    renamingMap: AssetRenamingMap,
    nameGenerator: NodeTokenStringGenerator,

    instructionLimit: Int? = null,
): AssetFileHandler {
    //region Lifecycles
    val timingLifecycle = Lifecycle.Timing()
    val timingLoggingLifecycle = object : Lifecycle {
        override suspend fun onFinished() {
            timingLogger.info { "Retrieval timing: \n$timingLifecycle" }
        }
    }

    val lifecycle = Lifecycle.Combined(
        timingLifecycle,
        timingLoggingLifecycle,
    )
    //endregion

    return FigmaAssetFileHandler(
        figmaFileDefinition = figmaFileDefinition,
        discoveryStrategy = NodeDiscoveryStrategy.JsonPath(jsonPath),
        assetFilter = assetFilter,
        lifecycle = lifecycle,
        instructionLimit = instructionLimit,
        seenNameTracker = renamingMap.asSeenNameTracker(),
    ) { canvas, node ->
        val namingContext = renamingMap.toNamingContext(canvas, node)
        listOf(
            Instruction.of(
                exportNode = node,
                exportConfig = exportConfig,
                importOutputName = nameGenerator.generate(namingContext),
                importPipeline = importPipeline,
            ),
        )
    }
}
