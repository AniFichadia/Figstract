package com.anifichadia.figstract.importer.variable.model.writer

import com.anifichadia.figstract.importer.variable.model.ThemeVariantMapping
import com.anifichadia.figstract.importer.variable.model.VariableData
import com.anifichadia.figstract.importer.variable.model.variableorganization.VariableOrganizationStrategy
import com.anifichadia.figstract.importer.variable.model.variabletree.VariableGroup

interface VariableDataWriter {
    suspend fun write(
        variableData: VariableData,
        themeVariantMapping: ThemeVariantMapping,
        organizationStrategy: VariableOrganizationStrategy,
        collectionName: String,
        root: VariableGroup,
    )
}
