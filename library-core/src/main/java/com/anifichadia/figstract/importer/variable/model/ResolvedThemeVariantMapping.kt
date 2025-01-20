package com.anifichadia.figstract.importer.variable.model

import com.anifichadia.figstract.figma.Number
import com.anifichadia.figstract.figma.model.Color
import com.anifichadia.figstract.importer.variable.model.VariableData.VariablesByMode

sealed interface ResolvedThemeVariantMapping {
    data class LightAndDark(
        val booleans: Map<String, Value<Boolean>>,
        val numbers: Map<String, Value<Number>>,
        val strings: Map<String, Value<String>>,
        val colors: Map<String, Value<Color>>,
    ) : ResolvedThemeVariantMapping {
        sealed interface Value<V> {
            fun resolve(isLight: Boolean): V

            data class LightOnly<V>(
                val light: V,
            ) : Value<V> {
                override fun resolve(isLight: Boolean): V = light
            }

            data class DarkOnly<V>(
                val dark: V,
            ) : Value<V> {
                override fun resolve(isLight: Boolean): V = dark
            }

            data class Both<V>(
                val light: V,
                val dark: V,
            ) : Value<V> {
                override fun resolve(isLight: Boolean): V = if (isLight) {
                    light
                } else {
                    dark
                }
            }
        }
    }

    data object None : ResolvedThemeVariantMapping
}

fun ThemeVariantMapping.resolve(variableData: VariableData): ResolvedThemeVariantMapping {
    return when (this) {
        is ThemeVariantMapping.LightAndDark -> this.resolve(variableData)
        is ThemeVariantMapping.None -> this.resolve(variableData)
    }
}

private fun ThemeVariantMapping.LightAndDark.resolve(variableData: VariableData): ResolvedThemeVariantMapping.LightAndDark {
    val lightMode =
        requireNotNull(variableData.variablesByMode.firstOrNull { it.mode.name == lightThemeModeName }) {
            "Mode matching light theme $lightThemeModeName not found in ${variableData.variableCollection.name}, modes ${variableData.variableCollection.modes.map { it.name }}"
        }
    val darkMode =
        requireNotNull(variableData.variablesByMode.firstOrNull { it.mode.name == darkThemeModeName }) {
            "Mode matching dark theme $darkThemeModeName not found in ${variableData.variableCollection.name}, modes ${variableData.variableCollection.modes.map { it.name }}"
        }

    return ResolvedThemeVariantMapping.LightAndDark(
        booleans = createResolvedMappings(lightMode, darkMode) { it.booleanVariables },
        numbers = createResolvedMappings(lightMode, darkMode) { it.numberVariables },
        strings = createResolvedMappings(lightMode, darkMode) { it.stringVariables },
        colors = createResolvedMappings(lightMode, darkMode) { it.colorVariables },
    )
}

private fun <V> ThemeVariantMapping.LightAndDark.createResolvedMappings(
    lightMode: VariablesByMode,
    darkMode: VariablesByMode,
    getMappings: (VariablesByMode) -> Map<String, V>?,
): Map<String, ResolvedThemeVariantMapping.LightAndDark.Value<V>> {
    val lightMappings = getMappings(lightMode)
    val darkMappings = getMappings(darkMode)
    if (lightMappings != null && darkMappings != null) {
        val resolved = (lightMappings.keys + darkMappings.keys).associateWith { colorName ->
            val lightValue = lightMappings[colorName]
            val darkValue = darkMappings[colorName]
            when {
                lightValue != null && darkValue != null -> ResolvedThemeVariantMapping.LightAndDark.Value.Both<V>(
                    light = lightValue,
                    dark = darkValue,
                )

                lightValue != null -> ResolvedThemeVariantMapping.LightAndDark.Value.LightOnly<V>(
                    light = lightValue,
                )

                darkValue != null -> ResolvedThemeVariantMapping.LightAndDark.Value.DarkOnly<V>(
                    dark = darkValue,
                )

                else -> error("This shouldn't be possible")
            }
        }

        return resolved
    } else {
        return emptyMap()
    }
}

private fun ThemeVariantMapping.None.resolve(variableData: VariableData): ResolvedThemeVariantMapping.None {
    return ResolvedThemeVariantMapping.None
}
