package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable

@Serializable
data class Constraint(
    val type: Type,
    val value: Double,
) {
    enum class Type {
        SCALE, WIDTH, HEIGHT,
    }
}
