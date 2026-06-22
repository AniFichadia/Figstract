package com.anifichadia.figstract.importer.asset.model

import com.anifichadia.figstract.figma.model.Node
import com.anifichadia.figstract.type.serializer.figmaSerializersModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import com.jayway.jsonpath.JsonPath as JaywayJsonPath


/**
 * Queries [Node.Canvas]'s serialized JSON representation using [expression] (a JsonPath expression), decoding each
 * match back into a [Node].
 */
@Suppress("FunctionName")
fun NodeDiscoveryStrategy.Companion.JsonPath(
    expression: String,
): NodeDiscoveryStrategy = NodeDiscoveryStrategy { canvas ->
    val canvasJson = jsonPathJson.encodeToString(canvas)

    val filteredJson = JaywayJsonPath
        .parse(canvasJson)
        .read<List<Map<String, Any>>>(expression)

    filteredJson
        .map { jsonMap -> jsonMap.toJsonElement() }
        .map { jsonPathJson.decodeFromJsonElement<Node>(it) }
}

private val jsonPathJson = Json {
    serializersModule = figmaSerializersModule
}

/**
 * This is a pile of unfortunate ... This helps interop between JsonPath's ... JSON representation and
 * kotlinx serialization
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
