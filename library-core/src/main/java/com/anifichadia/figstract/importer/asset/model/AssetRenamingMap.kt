package com.anifichadia.figstract.importer.asset.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A renaming map for assets, allowing canvas and/or node names to be overridden before name generation occurs.
 *
 * Keys for both maps are the old name (case-sensitive), and the values are the new name
 *
 * Both keys are optional. Entries not present in a map are left unchanged.
 */
@Serializable
data class AssetRenamingMap(
    val canvases: Map<String, String> = emptyMap(),
    val nodes: Map<String, String> = emptyMap(),
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        val Empty = AssetRenamingMap()

        fun fromFile(file: File): AssetRenamingMap {
            return json.decodeFromString<AssetRenamingMap>(file.readText())
        }
    }
}
