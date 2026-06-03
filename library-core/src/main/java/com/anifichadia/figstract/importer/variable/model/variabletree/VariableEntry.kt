package com.anifichadia.figstract.importer.variable.model.variabletree

data class VariableEntry<V : VariableValue>(
    val name: String,
    val figmaPath: String,
    val value: V,
)
