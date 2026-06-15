package com.anifichadia.figstract.importer.variable.model

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

private val logger = KotlinLogging.logger {}

@Serializable
data class VariableRenamingMap(
    val collections: Map<String, String> = emptyMap(),
    val variables: Map<String, Map<String, String>> = emptyMap(),
) {
    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

        val Empty = VariableRenamingMap()

        fun fromFile(file: File): VariableRenamingMap {
            return json.decodeFromString<VariableRenamingMap>(file.readText())
        }
    }
}

/**
 * Returns a copy of [variableData] with the collection name and all variable paths remapped.
 *
 * The collection name is remapped first via [collections]. The resolved (post-rename) collection name is then used to
 * look up a per-collection variable path map in [variables]. Variable paths are remapped across all modes and all
 * variable type maps inside [VariableData.VariablesByMode].
 */
fun VariableRenamingMap.applyTo(
    variableData: VariableData,
    seenCollectionNames: MutableSet<String>,
    seenVariablePaths: MutableMap<String, MutableSet<String>>,
): VariableData {
    val originalCollectionName = variableData.variableCollection.name
    seenCollectionNames += originalCollectionName
    val resolvedCollectionName = collections[originalCollectionName] ?: originalCollectionName
    val renamedCollection = variableData.variableCollection.copy(name = resolvedCollectionName)

    val variableRemap = variables[resolvedCollectionName] ?: emptyMap()
    val seenPaths = seenVariablePaths.getOrPut(resolvedCollectionName) { mutableSetOf() }

    val renamedVariablesByMode = variableData.variablesByMode.map { byMode ->
        byMode.copy(
            booleanVariables = byMode.booleanVariables?.remapKeys(seenPaths, variableRemap),
            numberVariables = byMode.numberVariables?.remapKeys(seenPaths, variableRemap),
            stringVariables = byMode.stringVariables?.remapKeys(seenPaths, variableRemap),
            colorVariables = byMode.colorVariables?.remapKeys(seenPaths, variableRemap),
        )
    }

    return variableData.copy(
        variableCollection = renamedCollection,
        variablesByMode = renamedVariablesByMode,
    )
}

private fun <V> Map<String, V>.remapKeys(
    seen: MutableSet<String>,
    remap: Map<String, String>,
): Map<String, V> = entries.associate { (key, value) ->
    seen += key
    (remap[key] ?: key) to value
}

fun VariableRenamingMap.warnUnused(
    seenCollectionNames: Set<String>,
    seenVariablePaths: Map<String, Set<String>>,
) {
    val unusedCollections = collections.keys - seenCollectionNames
    if (unusedCollections.isNotEmpty()) {
        logger.warn {
            "Variable renaming map contains collection entries that did not match any collection: $unusedCollections"
        }
    }

    for ((collectionName, pathRemap) in variables) {
        val seen = seenVariablePaths[collectionName] ?: emptySet()
        val unusedPaths = pathRemap.keys - seen
        if (unusedPaths.isNotEmpty()) {
            logger.warn {
                "Variable renaming map contains variable entries for collection \"$collectionName\" " +
                    "that did not match any variable path: $unusedPaths"
            }
        }
    }

    val unmatchedCollections = variables.keys - (seenVariablePaths.keys + collections.values)
    if (unmatchedCollections.isNotEmpty()) {
        logger.warn {
            "Variable renaming map contains variable remaps for collections that were never seen: $unmatchedCollections"
        }
    }
}
