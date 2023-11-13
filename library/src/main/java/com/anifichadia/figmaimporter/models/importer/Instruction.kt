package com.anifichadia.figmaimporter.models.importer

import com.anifichadia.figmaimporter.models.figma.NodeId
import com.anifichadia.figmaimporter.models.importer.exporting.ExportConfig
import com.anifichadia.figmaimporter.models.importer.importing.Destination
import com.anifichadia.figmaimporter.models.importer.importing.ImportPipelineStep

data class Instruction(
    val export: Export,
    val import: Import,
) {
    data class Export(
        val nodeId: NodeId,
        val config: ExportConfig,
    )

    data class Import(
        val outputName: String,
        val before: ImportPipelineStep = ImportPipelineStep.passthrough,
        val destination: Destination,
    ) {
        val pipelineSteps = listOf(before, destination)
    }

    companion object {
        fun of(
            exportNodeId: NodeId,
            exportConfig: ExportConfig,
            importOutputName: String,
            importBefore: ImportPipelineStep = ImportPipelineStep.passthrough,
            importDestination: Destination,
        ): Instruction {
            return Instruction(
                export = Export(
                    nodeId = exportNodeId,
                    config = exportConfig,
                ),
                import = Import(
                    outputName = importOutputName,
                    before = importBefore,
                    destination = importDestination,
                ),
            )
        }
    }
}
