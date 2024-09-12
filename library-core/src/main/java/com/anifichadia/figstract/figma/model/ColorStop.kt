package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class ColorStop(
    val position: Number,
    val color: Color,
)
