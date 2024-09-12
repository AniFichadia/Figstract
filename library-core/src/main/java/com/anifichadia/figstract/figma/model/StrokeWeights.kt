package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import kotlinx.serialization.Serializable

@Serializable
class StrokeWeights (
    val top: Number,
    val right: Number,
    val bottom: Number,
    val left: Number,
)
