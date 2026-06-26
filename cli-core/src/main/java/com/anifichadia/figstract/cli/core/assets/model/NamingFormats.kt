package com.anifichadia.figstract.cli.core.assets.model

import com.anifichadia.figstract.Conventions
import com.anifichadia.figstract.model.TokenStringGenerator
import com.anifichadia.figstract.web.web
import kotlinx.serialization.Serializable

@Serializable
data class NamingFormats(
    val androidFormat: String,
    // Note: casing intentionally non-configurable on android
    val iosFormat: String,
    // Note: casing intentionally non-configurable on iOS
    val webFormat: String,
    val webCasing: TokenStringGenerator.Casing = Conventions.Casing.web,
)
