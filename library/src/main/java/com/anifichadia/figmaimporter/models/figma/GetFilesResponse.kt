package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable

@Serializable
data class GetFilesResponse(
    val name: String,
    val role: String,
    val lastModified: String,
    val editorType: String,
    val thumbnailUrl: String,
    val version: String,
    val document: Node.Document,
    val components: Map<String, Component>,
    val componentSets: Map<String, ComponentSet>,
    val schemaVersion: Int,
    val styles: Map<String, Style>,
    val linkAccess: String,
)
