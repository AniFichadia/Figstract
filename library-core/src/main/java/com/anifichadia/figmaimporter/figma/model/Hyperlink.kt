package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Hyperlink(
    val type: Type,
    val url: String? = null,
    val nodeID: String? = null,
) {
    enum class Type {
        URL,
        NODE,
        ;
    }
}
