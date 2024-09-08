package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
data class VariableCollection(
    val id: String,
    val name: String,
    val key: String,
    val modes: List<Mode>,
    val variableIds: List<String>,
)
