package com.anifichadia.figmaimporter.cli.core.assets

import com.anifichadia.figmaimporter.util.ToUpperCamelCase
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.boolean

class PlatformOptionGroup : OptionGroup() {
    val androidEnabled by createOption("android")
    val iosEnabled by createOption("ios")
    val webEnabled by createOption("web")

    fun noneEnabled() = !androidEnabled && !iosEnabled && !webEnabled

    private companion object {
        fun OptionGroup.createOption(platformName: String): OptionWithValues<Boolean, Boolean, Boolean> {
            val platformNameForOption = platformName.ToUpperCamelCase()

            return option("--platform$platformNameForOption")
                .boolean()
                .default(DEFAULT_VALUE)
        }

        private const val DEFAULT_VALUE = false
    }
}
