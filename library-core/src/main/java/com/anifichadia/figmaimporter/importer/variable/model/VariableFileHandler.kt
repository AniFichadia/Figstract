package com.anifichadia.figmaimporter.importer.variable.model

import com.anifichadia.figmaimporter.figma.FileKey

data class VariableFileHandler(
    val figmaFile: FileKey,
    val filter: VariableFileFilter,
    val writers: List<VariableDataWriter>,
)
