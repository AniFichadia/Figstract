package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.FileKey

data class VariableFileHandler(
    val figmaFile: FileKey,
    val filter: VariableFileFilter,
    val writers: List<VariableDataWriter>,
)