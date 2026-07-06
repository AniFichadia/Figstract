package com.anifichadia.figstract.cli.core.assets.option

import com.anifichadia.figstract.cli.core.assets.model.PlatformOptions
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean

class PlatformOptionGroup : OptionGroup() {
    val androidEnabled by createOption("android")
    val iosEnabled by createOption("ios")
    val webEnabled by createOption("web")

    companion object {
        private fun OptionGroup.createOption(platformName: String): OptionWithValues<Boolean, Boolean, Boolean> {
            val platformNameForOption = platformName.ToUpperCamelCase()

            return option("--platform$platformNameForOption")
                .boolean()
                .default(DEFAULT_VALUE)
        }

        private const val DEFAULT_VALUE = false

        fun PlatformOptionGroup.toPlatformOptions() = PlatformOptions(
            androidEnabled = androidEnabled,
            iosEnabled = iosEnabled,
            webEnabled = webEnabled,
        )
    }
}
