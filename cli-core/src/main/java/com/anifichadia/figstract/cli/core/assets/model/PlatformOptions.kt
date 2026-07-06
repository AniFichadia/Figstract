package com.anifichadia.figstract.cli.core.assets.model

import com.anifichadia.figstract.type.fold
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class PlatformOptions(
    val androidEnabled: Boolean = false,
    val iosEnabled: Boolean = false,
    val webEnabled: Boolean = false,
) {
    fun noneEnabled() = !androidEnabled && !iosEnabled && !webEnabled

    fun androidDirectory(outDirectory: File) = outDirectory.fold("android").takeIf { androidEnabled }

    fun iosDirectory(outDirectory: File) = outDirectory.fold("ios").takeIf { iosEnabled }

    fun webDirectory(outDirectory: File) = outDirectory.fold("web").takeIf { webEnabled }
}
