package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class Color(
    val r: Number,
    val g: Number,
    val b: Number,
    val a: Number,
)
