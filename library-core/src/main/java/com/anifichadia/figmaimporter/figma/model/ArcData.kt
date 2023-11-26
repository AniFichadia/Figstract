package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable
import com.anifichadia.figmaimporter.figma.Number

@Serializable
data class ArcData(
    val startingAngle: Number,
    val endingAngle: Number,
    val innerRadius: Number,
)
