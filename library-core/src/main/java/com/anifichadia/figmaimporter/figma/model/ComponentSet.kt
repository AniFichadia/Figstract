package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class ComponentSet(
    val key: String,
    val name: String,
    val description: String,
    val documentationLinks: List<DocumentationLink> = emptyList(),
    val remote: Boolean,
)
