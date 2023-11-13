package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

enum class BlendMode {
    // Normal blends
    PASS_THROUGH,
    NORMAL,
    // Darken
    DARKEN,
    MULTIPLY,
    LINEAR_BURN,
    COLOR_BURN,
    // Lighten
    LIGHTEN,
    SCREEN,
    LINEAR_DODGE,
    COLOR_DODGE,
    // Contrast
    OVERLAY,
    SOFT_LIGHT,
    HARD_LIGHT,
    // Inversion
    DIFFERENCE,
    EXCLUSION,
    // Component
    HUE,
    SATURATION,
    COLOR,
    LUMINOSITY,
    ;
}
