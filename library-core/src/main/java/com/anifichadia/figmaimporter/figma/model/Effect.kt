package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
class Effect(
    val type: Type,
    val visible: Boolean,
    val radius: Number,
    val color: Color,
    val blendMode: BlendMode,
    val offset: Vector,
    val spread: Number = 0.0,
    val shadowBehindNode: Boolean,
) {
    enum class Type {
        INNER_SHADOW,
        DROP_SHADOW,
        LAYER_BLUR,
        BACKGROUND_BLUR,
        ;
    }
}
