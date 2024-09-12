package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class Error(
    val status: Int,
    val err: String,
)
