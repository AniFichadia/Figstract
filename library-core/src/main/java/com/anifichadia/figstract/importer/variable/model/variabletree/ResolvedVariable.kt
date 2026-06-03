package com.anifichadia.figstract.importer.variable.model.variabletree

data class ResolvedVariable(
    val name: String,
    /** Preserves the original raw Figma path for logging and diagnostics. */
    val figmaPath: String,
)
