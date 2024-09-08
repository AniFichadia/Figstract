package com.anifichadia.figmaimporter.figma.model

import kotlinx.serialization.Serializable

@Serializable
enum class ResolvedType {
    BOOLEAN, FLOAT, STRING, COLOR
}
