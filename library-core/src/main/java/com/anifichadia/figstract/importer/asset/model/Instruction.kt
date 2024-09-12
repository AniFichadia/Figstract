package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.NodeId
import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.importer.asset.model.exporting.ExportConfig
import com.anifichadia.figstract.importer.asset.model.importing.ImportPipeline

data class Instruction(
    val export: Export,
    val import: Import,
) {
    data class Export(
        val nodeId: NodeId,
        val nodeName: String,
        val config: ExportConfig,
    )

    data class Import(
        val importTarget: ImportTarget.Initial,
        val pipeline: ImportPipeline = ImportPipeline(ImportPipeline.Step.PassThrough),
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
            exportNode: Node,
            exportConfig: ExportConfig,
            importOutputName: String,
            importPipeline: ImportPipeline,
        ): Instruction {
            return Instruction(
                export = Export(
                    nodeId = exportNode.id,
                    nodeName = exportNode.name,
                    config = exportConfig,
                ),
                import = Import(
                    importTarget = ImportTarget.Initial(outputName = importOutputName),
                    pipeline = importPipeline,
                ),
            )
        }

        fun of(
            exportNode: Node,
            exportConfig: ExportConfig,
            importTarget: ImportTarget.Initial,
            importPipeline: ImportPipeline,
        ): Instruction {
            return Instruction(
                export = Export(
                    nodeId = exportNode.id,
                    nodeName = exportNode.name,
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
            exportNode: Node,
            exportConfig: ExportConfig,
            importOutputName: String,
            importPipeline: ImportPipeline,
        ) {
            add(
                of(
                    exportNode = exportNode,
                    exportConfig = exportConfig,
                    importOutputName = importOutputName,
                    importPipeline = importPipeline,
                )
            )
        }

        fun MutableList<Instruction>.addInstruction(
            exportNode: Node,
            exportConfig: ExportConfig,
            importTarget: ImportTarget.Initial,
            importPipeline: ImportPipeline,
        ) {
            add(
                of(
                    exportNode = exportNode,
                    exportConfig = exportConfig,
                    importTarget = importTarget,
                    importPipeline = importPipeline,
                )
            )
        }

        fun ExportSetting.toInstruction(
            exportNode: Node,
            exportScale: Float = ExportConfig.SCALE_ORIGINAL,
            importOutputName: String,
            importPipeline: ImportPipeline,
        ): Instruction {
            return of(
                exportNode = exportNode,
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
            exportNode: Node,
            exportScale: Float = ExportConfig.SCALE_ORIGINAL,
            importTarget: ImportTarget.Initial,
            importPipeline: ImportPipeline,
        ): Instruction {
            return of(
                exportNode = exportNode,
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
