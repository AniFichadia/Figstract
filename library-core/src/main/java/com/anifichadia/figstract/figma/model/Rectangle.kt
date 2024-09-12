package com.anifichadia.figstract.figma.model

import com.anifichadia.figstract.figma.Number
import kotlinx.serialization.Serializable

@Serializable
data class Rectangle(
    val x: Number,
    val y: Number,
    val width: Number,
    val height: Number,
)
