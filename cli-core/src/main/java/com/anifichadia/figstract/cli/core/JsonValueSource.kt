package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.InvalidFileFormat
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ValueSource
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * A [ValueSource] that uses Kotlin serialization to parse JSON files
 */
class JsonValueSource(
    private val root: JsonObject,
    private val getKey: (Context, Option) -> String = ValueSource.getKey(joinSubcommands = "."),
) : ValueSource {
    override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> {
        var cursor: JsonElement? = root
        val parts = (option.valueSourceKey ?: getKey(context, option)).split(".")
        for (part in parts) {
            if (cursor !is JsonObject) return emptyList()
            cursor = cursor[part]
        }
        if (cursor == null) return emptyList()

        try {
            // This implementation interprets a list as multiple invocations, but you could also
            // implement it as a single invocation with multiple values.
            if (cursor is JsonArray) return cursor.map {
                ValueSource.Invocation.value(it.jsonPrimitive.content)
            }
            return ValueSource.Invocation.just(cursor.jsonPrimitive.content)
        } catch (e: IllegalArgumentException) {
            // This implementation skips invalid values, but you could handle them differently.
            return emptyList()
        }
    }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        fun defaultJson() = Json {
            allowComments = true
        }

        fun from(
            file: File,
            json: Json = defaultJson(),
            requireValid: Boolean = false,
            getKey: (Context, Option) -> String = ValueSource.getKey(joinSubcommands = "."),
        ): JsonValueSource {
            if (!file.isFile) return JsonValueSource(JsonObject(emptyMap()), getKey)

            val jsonOutput = try {
                json.parseToJsonElement(file.readText()) as? JsonObject
                    ?: throw InvalidFileFormat(file.path, "object expected", 1)
            } catch (e: SerializationException) {
                if (requireValid) {
                    throw InvalidFileFormat(file.name, e.message ?: "could not read file")
                }
                JsonObject(emptyMap())
            }
            return JsonValueSource(jsonOutput, getKey)
        }
    }
}
