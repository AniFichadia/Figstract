package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Mode(
    val modeId: String,
    val name: String,
)
