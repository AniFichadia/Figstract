package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable
import kotlin.Int

@Serializable
data class Color(
    val r: Int,
    val g: Int,
    val b: Int,
    val a: Int
)
