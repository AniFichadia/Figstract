package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.importer.Lifecycle
import com.anifichadia.figstract.type.serializer.figmaSerializersModule
import com.jayway.jsonpath.JsonPath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement


@Suppress("FunctionName")
fun JsonPathAssetFileHandler(
    figmaFile: FileKey,
    jsonPath: String,
    assetsPerChunk: Int = AssetFileHandler.DEFAULT_ASSETS_PER_CHUNK,
    lifecycle: Lifecycle = Lifecycle.NoOp,
    canvasFilter: (Node.Canvas) -> Boolean = { true },
    nodeFilter: (Node) -> Boolean = { true },
    createInstructions: (node: Node, canvas: Node.Canvas) -> List<Instruction>,
): AssetFileHandler {
    val json = Json {
        serializersModule = figmaSerializersModule
    }

    return AssetFileHandler(
        figmaFile = figmaFile,
        assetsPerChunk = assetsPerChunk,
        lifecycle = lifecycle,
    ) { response, _ ->
        val canvases = response.document.children
            .filterIsInstance<Node.Canvas>()
            .filter(canvasFilter)

        canvases
            .map { canvas ->
                val canvasJson = json.encodeToString(canvas)

                val filteredJson = JsonPath
                    .parse(canvasJson)
                    .read<List<Map<String, Any>>>(jsonPath)
                val unfilteredNodes = filteredJson
                    .map { jsonMap -> jsonMap.toJsonElement() }
                    .map { json.decodeFromJsonElement<Node>(it) }
                val nodes = unfilteredNodes
                    .filter(nodeFilter)

                val instructions = nodes
                    .map { createInstructions(it, canvas) }
                    .flatten()

                instructions
            }
            .flatten()
    }
}

/**
 * This is a pile of unfortunate ... This helps interop between JsonPath's ... JSON representation and kotlinx serialization
 */
private fun Any.toJsonElement(): JsonElement {
    return when (this) {
        is List<*> -> {
            val content = this.mapNotNull { it?.toJsonElement() }
            JsonArray(content)
        }

        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            val content = (this as Map<String, Any>).entries.associate { (key, value) ->
                key to value.toJsonElement()
            }
            JsonObject(content)
        }

        is Boolean -> JsonPrimitive(this)
        is Int -> JsonPrimitive(this)
        is Float -> JsonPrimitive(this)
        is Double -> JsonPrimitive(this)
        is String -> JsonPrimitive(this)
        else -> JsonPrimitive(this.toString())
    }
}
