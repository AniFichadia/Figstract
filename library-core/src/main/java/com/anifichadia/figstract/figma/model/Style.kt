package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Style(
    val key: String,
    val name: String,
    val description: String,
    val remote: Boolean,
    val styleType: Type,
) {
    enum class Type {
        FILL,
        TEXT,
        EFFECT,
        GRID,
        ;
    }
}
