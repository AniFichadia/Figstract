package com.anifichadia.figmaimporter.models.figma

import kotlinx.serialization.Serializable

// TODO: create ExportConfig from this
@Serializable
data class ExportSetting(
    val suffix: String?,
    val format: String,
    val constraint: Constraint,
)
