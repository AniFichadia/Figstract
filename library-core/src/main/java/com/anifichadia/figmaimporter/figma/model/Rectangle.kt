package com.anifichadia.figmaimporter.figma.model

import com.anifichadia.figmaimporter.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class Rectangle(
    val x: Number,
    val y: Number,
    val width: Number,
    val height: Number,
)
