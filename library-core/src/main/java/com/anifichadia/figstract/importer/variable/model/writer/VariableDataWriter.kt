package com.anifichadia.figstract.importer.variable.model.writer

import com.anifichadia.figstract.importer.variable.model.ResolvedThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData

interface VariableDataWriter {
    suspend fun write(
        variableData: VariableData,
        resolvedThemeVariantMapping: ResolvedThemeVariantMapping,
    )
}
