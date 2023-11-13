package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class ColorStop(
    val position: Number,
    val color: Color,
)
