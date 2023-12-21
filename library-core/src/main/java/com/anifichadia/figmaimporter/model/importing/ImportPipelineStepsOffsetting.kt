package com.anifichadia.figmaimporter.model.importing

import com.anifichadia.figmaimporter.figma.Number
import com.anifichadia.figmaimporter.figma.model.Rectangle
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolveOutputName
import com.anifichadia.figmaimporter.model.importing.ImportPipeline.Step.Companion.resolvePathElements
import com.anifichadia.figmaimporter.type.fold
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.annotations.ApiStatus.Experimental
import java.io.File

@Experimental
fun createOffsetInfo(
    directory: File,
    parent: Rectangle,
    node: Rectangle,
) = ImportPipeline.Step(
    "createOffsetInfo(directory: $directory, parent: $parent,node: $node",
) { instruction, input ->
    val outputName = resolveOutputName(instruction, input)
    val outputPathElements = resolvePathElements(instruction, input)

    val offsetInfo = OffsetInfo(
        name = outputName,
        width = parent.height,
        height = parent.height,
        topLeftOffsetX = (node.x - parent.x).toFloat(),
        topLeftOffsetY = (node.y - parent.y).toFloat(),
        bottomRightOffsetX = ((parent.x + parent.width) - (node.x + node.width)).toFloat(),
        bottomRightOffsetY = ((parent.y + parent.height) - (node.y + node.height)).toFloat(),
    )

    val outputFile = directory.fold(outputPathElements, "${outputName}_offset.json")
    outputFile.parentFile.mkdirs()

    outputFile.writeText(json.encodeToString(offsetInfo))

    ImportPipeline.Output.none
}

@Serializable
private data class OffsetInfo(
    val name: String,
    val width: Number,
    val height: Number,
    val topLeftOffsetX: Float,
    val topLeftOffsetY: Float,
    val bottomRightOffsetX: Float,
    val bottomRightOffsetY: Float,
)

private val json = Json {
    prettyPrint = true
}
