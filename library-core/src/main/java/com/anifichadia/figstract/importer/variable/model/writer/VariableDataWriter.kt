package com.anifichadia.figstract.importer.variable.model.writer

import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variablenaming.VariableNamingStrategy
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup

interface VariableDataWriter {
    suspend fun write(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        namingStrategy: VariableNamingStrategy,
        collectionName: String,
        root: VariableGroup,
    )
}
