package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class ArcData(
    val startingAngle: Number,
    val endingAngle: Number,
    val innerRadius: Number,
)
