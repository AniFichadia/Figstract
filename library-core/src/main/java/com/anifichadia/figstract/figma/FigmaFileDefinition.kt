package com.anifichadia.figstract.figma

data class FigmaFileDefinition(
    val fileKey: FileKey,
    val branchName: String?,
    val version: String?,
)
