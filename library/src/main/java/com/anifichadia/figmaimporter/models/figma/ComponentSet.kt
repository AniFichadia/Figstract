package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable

@Serializable
data class ComponentSet(
    val key: String,
    val name: String,
    val description: String,
//    val documentationLinks: Array<Any>
    val remote: Boolean,
)
