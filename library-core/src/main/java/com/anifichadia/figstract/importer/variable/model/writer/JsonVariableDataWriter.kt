package com.anifichadia.figstract.importer.variable.model.writer

import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableTypeBucket
import com.anifichadia.figstract.util.sanitiseFileName
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File

class JsonVariableDataWriter(
    private val outDirectory: File,
    private val colorAsHex: Boolean = true,
) : VariableDataWriter {
    init {
        require(!outDirectory.exists() || outDirectory.isDirectory) {
            "outDirectory must be a directory: $outDirectory"
        }
    }

    override suspend fun write(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
        collectionName: String,
        root: VariableGroup,
    ) {
        val outputFile = outDirectory.resolve("${collectionName.sanitiseFileName()}.json")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(json.encodeToString(JsonElement.serializer(), buildGroupJson(root)))
    }

    private fun buildGroupJson(group: VariableGroup): JsonObject {
        val entries = mutableMapOf<String, JsonElement>()

        group.children.forEach { child ->
            entries[child.name] = buildGroupJson(child)
        }

        group.buckets.forEach { bucket ->
            entries[bucket.name] = buildBucketJson(bucket)
        }

        return JsonObject(entries)
    }

    private fun buildBucketJson(bucket: VariableTypeBucket): JsonObject {
        return when (bucket) {
            is VariableTypeBucket.Single.Booleans ->
                JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.value.value) })

            is VariableTypeBucket.Single.Numbers ->
                JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.value.value) })

            is VariableTypeBucket.Single.Strings ->
                JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.value.value) })

            is VariableTypeBucket.Single.Colors ->
                JsonObject(bucket.entries.associate { it.name to it.value.value.toJson() })

            is VariableTypeBucket.LightAndDark.Booleans -> JsonObject(
                mapOf(
                    "light" to JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.light.value.value) }),
                    "dark" to JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.dark.value.value) }),
                )
            )

            is VariableTypeBucket.LightAndDark.Numbers -> JsonObject(
                mapOf(
                    "light" to JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.light.value.value) }),
                    "dark" to JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.dark.value.value) }),
                )
            )

            is VariableTypeBucket.LightAndDark.Strings -> JsonObject(
                mapOf(
                    "light" to JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.light.value.value) }),
                    "dark" to JsonObject(bucket.entries.associate { it.name to JsonPrimitive(it.dark.value.value) }),
                )
            )

            is VariableTypeBucket.LightAndDark.Colors -> JsonObject(
                mapOf(
                    "light" to JsonObject(bucket.entries.associate { it.name to it.light.value.value.toJson() }),
                    "dark" to JsonObject(bucket.entries.associate { it.name to it.dark.value.value.toJson() }),
                )
            )
        }
    }

    private fun Color.toJson(): JsonElement {
        return if (colorAsHex) {
            JsonPrimitive(toHexString())
        } else {
            JsonObject(
                mapOf(
                    "r" to JsonPrimitive(r),
                    "g" to JsonPrimitive(g),
                    "b" to JsonPrimitive(b),
                    "a" to JsonPrimitive(a),
                )
            )
        }
    }

    private companion object {
        private val json = Json {
            prettyPrint = true
            explicitNulls = false
        }
    }
}
