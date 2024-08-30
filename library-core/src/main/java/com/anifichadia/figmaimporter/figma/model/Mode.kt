package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Mode(
    val modeId: String,
    val name: String,
)
