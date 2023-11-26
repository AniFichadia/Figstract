package com.anifichadia.figmaimporter.model

import com.anifichadia.figmaimporter.figma.NodeId
import com.anifichadia.figmaimporter.figma.model.ExportSetting
import com.anifichadia.figmaimporter.model.exporting.ExportConfig
import com.anifichadia.figmaimporter.model.importing.Destination
import com.anifichadia.figmaimporter.model.importing.ImportPipeline

data class Instruction(
    val export: Export,
    val import: Import,
) {
    data class Export(
        val nodeId: NodeId,
        val config: ExportConfig,
    )

    data class Import(
        val importTarget: ImportTarget.Initial,
        val pipeline: ImportPipeline = ImportPipeline(ImportPipeline.Step.PassThrough, Destination.None),
    )

    sealed interface ImportTarget {
        val pathElements: List<String>
        val outputName: String?
        val format: String?

        data class Initial(
            override val outputName: String,
            override val pathElements: List<String> = emptyList(),
            override val format: String? = null,
        ) : ImportTarget

        data class Override(
            override val outputName: String? = null,
            override val pathElements: List<String> = emptyList(),
            override val format: String? = null,
        ) : ImportTarget

        companion object {
            fun Initial.merge(override: Override): Initial {
                return this.copy(
                    outputName = override.outputName ?: outputName,
                    pathElements = this.pathElements + override.pathElements,
                    format = override.format ?: format,
                )
            }
        }
    }

    companion object {
        fun of(
            exportNodeId: NodeId,
            exportConfig: ExportConfig,
            importOutputName: String,
            importPipeline: ImportPipeline,
        ): Instruction {
            return Instruction(
                export = Export(
                    nodeId = exportNodeId,
                    config = exportConfig,
                ),
                import = Import(
                    importTarget = ImportTarget.Initial(outputName = importOutputName),
                    pipeline = importPipeline,
                ),
            )
        }

        fun of(
            exportNodeId: NodeId,
            exportConfig: ExportConfig,
            importTarget: ImportTarget.Initial,
            importPipeline: ImportPipeline,
        ): Instruction {
            return Instruction(
                export = Export(
                    nodeId = exportNodeId,
                    config = exportConfig,
                ),
                import = Import(
                    importTarget = importTarget,
                    pipeline = importPipeline,
                ),
            )
        }

        fun buildInstructions(builderAction: MutableList<Instruction>.() -> Unit) = buildList(builderAction)

        fun MutableList<Instruction>.addInstruction(
            exportNodeId: NodeId,
            exportConfig: ExportConfig,
            importOutputName: String,
            importPipeline: ImportPipeline,
        ) {
            add(
                of(
                    exportNodeId = exportNodeId,
                    exportConfig = exportConfig,
                    importOutputName = importOutputName,
                    importPipeline = importPipeline,
                )
            )
        }

        fun MutableList<Instruction>.addInstruction(
            exportNodeId: NodeId,
            exportConfig: ExportConfig,
            importTarget: ImportTarget.Initial,
            importPipeline: ImportPipeline,
        ) {
            add(
                of(
                    exportNodeId = exportNodeId,
                    exportConfig = exportConfig,
                    importTarget = importTarget,
                    importPipeline = importPipeline,
                )
            )
        }

        fun ExportSetting.toInstruction(
            exportNodeId: NodeId,
            exportScale: Float = ExportConfig.SCALE_ORIGINAL,
            importOutputName: String,
            importPipeline: ImportPipeline,
        ): Instruction {
            return of(
                exportNodeId = exportNodeId,
                exportConfig = ExportConfig(
                    format = format,
                    scale = exportScale,
                ),
                importOutputName = listOfNotNull(
                    suffix,
                    importOutputName,
                ).joinToString(separator = ""),
                importPipeline = importPipeline,
            )
        }

        fun ExportSetting.toInstruction(
            exportNodeId: NodeId,
            exportScale: Float = ExportConfig.SCALE_ORIGINAL,
            importTarget: ImportTarget.Initial,
            importPipeline: ImportPipeline,
        ): Instruction {
            return of(
                exportNodeId = exportNodeId,
                exportConfig = ExportConfig(
                    format = format,
                    scale = exportScale,
                ),
                importTarget = importTarget.copy(
                    outputName = listOfNotNull(
                        suffix,
                        importTarget.outputName,
                    ).joinToString(separator = ""),
                ),
                importPipeline = importPipeline,
            )
        }
    }
}
