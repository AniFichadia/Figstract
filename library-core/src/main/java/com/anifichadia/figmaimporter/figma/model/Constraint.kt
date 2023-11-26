package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class Constraint(
    val type: Type,
    val value: Number,
) {
    enum class Type {
        SCALE,
        WIDTH,
        HEIGHT,
        ;
    }
}
