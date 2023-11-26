package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class LayoutConstraint(
    val vertical: Vertical,
    val horizontal: Horizontal,
) {
    enum class Vertical {
        TOP,
        BOTTOM,
        CENTER,
        TOP_BOTTOM,
        SCALE,
        ;
    }

    enum class Horizontal {
        LEFT,
        RIGHT,
        CENTER,
        LEFT_RIGHT,
        SCALE,
        ;
    }
}
