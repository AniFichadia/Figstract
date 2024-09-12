package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Component(
    val key: String,
    val name: String,
    val description: String,
    val componentSetId: String? = null,
    val documentationLinks: List<DocumentationLink> = emptyList(),
    val remote: Boolean,
)
