package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable

@Serializable
data class Component(
    val key: String,
    val name: String,
    val description: String,
    val componentSetId: String? = null,
//    val documentationLinks: Array<Any>
    val remote: Boolean,
)
