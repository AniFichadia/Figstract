package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
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
