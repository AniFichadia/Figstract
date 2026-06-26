package com.anifichadia.figstract.figma

import kotlinx.serialization.Serializable

@Serializable
data class FigmaFileDefinition(
    val fileKey: FileKey,
    val branchName: String? = null,
    val version: String? = null,
)
