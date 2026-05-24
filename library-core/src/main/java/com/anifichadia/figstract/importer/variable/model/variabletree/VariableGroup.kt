package com.anifichadia.figstract.importer.variable.model.variabletree

data class VariableGroup(
    val name: String,
    val children: List<VariableGroup> = emptyList(),
    val buckets: List<VariableTypeBucket> = emptyList(),
)
