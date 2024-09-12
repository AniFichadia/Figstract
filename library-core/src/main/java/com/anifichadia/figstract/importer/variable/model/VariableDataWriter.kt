package com.anifichadia.figstract.importer.variable.model

interface VariableDataWriter {
    suspend fun write(variableData: VariableData)
}
