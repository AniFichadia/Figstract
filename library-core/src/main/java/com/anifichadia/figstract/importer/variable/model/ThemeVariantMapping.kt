package com.anifichadia.figstract.importer.variable.model

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Polymorphic
@Serializable
sealed interface ThemeVariantMapping {
    @Serializable
    @SerialName("LightAndDark")
    data class LightAndDark(
        val lightThemeModeName: String,
        val darkThemeModeName: String,
    ) : ThemeVariantMapping

    @Serializable
    @SerialName("None")
    data object None : ThemeVariantMapping
}
