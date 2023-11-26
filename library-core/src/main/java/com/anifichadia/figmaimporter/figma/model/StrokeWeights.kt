package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
class StrokeWeights (
    val top: Number,
    val right: Number,
    val bottom: Number,
    val left: Number,
)
