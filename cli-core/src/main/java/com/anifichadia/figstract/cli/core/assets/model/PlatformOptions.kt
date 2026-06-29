package com.anifichadia.figstract.cli.core.assets.model

import kotlinx.serialization.Serializable

@Serializable
data class PlatformOptions(
    val androidEnabled: Boolean = true,
    val iosEnabled: Boolean = true,
    val webEnabled: Boolean = true,
)
