package com.anifichadia.figstract.importer.variable.model.variabletree

data class LightDarkEntry<V : VariableValue>(
    val light: VariableEntry<V>,
    val dark: VariableEntry<V>,
) {
    val name: String get() = light.name
    val figmaPath: String get() = light.figmaPath
}
