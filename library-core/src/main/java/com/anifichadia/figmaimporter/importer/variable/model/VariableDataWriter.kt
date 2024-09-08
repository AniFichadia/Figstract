package com.anifichadia.figmaimporter.importer.variable.model

interface VariableDataWriter {
    suspend fun write(variableData: VariableData)
}
