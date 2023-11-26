package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val status: Int,
    val err: String,
)
