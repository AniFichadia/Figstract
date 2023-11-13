package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable
import com.anifichadia.figmaimporter.figma.Number

@Serializable
data class Rectangle(
    val x: Number,
    val y: Number,
    val width: Number,
    val height: Number,
)
