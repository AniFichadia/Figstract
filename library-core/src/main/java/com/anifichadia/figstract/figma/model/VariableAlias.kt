package com.anifichadia.figstract.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class VariableAlias(
    val type: String,
    val id: String,
)
