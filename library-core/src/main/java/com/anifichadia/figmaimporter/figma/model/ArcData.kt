package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class ArcData(
    val startingAngle: Number,
    val endingAngle: Number,
    val innerRadius: Number,
)
