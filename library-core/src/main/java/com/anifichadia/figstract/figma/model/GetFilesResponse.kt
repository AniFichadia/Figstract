package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime

@Serializable
data class GetFilesResponse(
    val name: String,
    val role: String,
    @Contextual
    val lastModified: OffsetDateTime,
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
