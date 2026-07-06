package com.anifichadia.figstract.cli.core.assets.model

import kotlinx.serialization.Serializable

@Serializable
data class PlatformOptions(
    val androidEnabled: Boolean = false,
    val iosEnabled: Boolean = false,
    val webEnabled: Boolean = false,
)
