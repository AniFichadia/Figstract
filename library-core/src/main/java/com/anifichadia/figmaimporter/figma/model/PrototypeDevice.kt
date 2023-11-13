package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
class PrototypeDevice(
    val type: Type,
    val size: Size? = null,
    val presetIdentifier: String? = null,
    val rotation: Rotation,
) {
    enum class Type {
        NONE,
        PRESET,
        CUSTOM,
        PRESENTATION,
        ;
    }

    enum class Rotation {
        NONE,
        CCW_90,
        ;
    }
}
