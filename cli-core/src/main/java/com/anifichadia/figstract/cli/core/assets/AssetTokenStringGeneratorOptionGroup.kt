package com.anifichadia.figstract.cli.core.assets

import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.model.TokenStringGenerator.Casing
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option

class AssetTokenStringGeneratorOptionGroup(
    prefix: String,
    androidFormat: String,
    iosFormat: String,
    webFormat: String,
) : DelegatableOptionGroup() {
    val android by createOption(
        prefix = "${prefix}Android",
        defaultFormat = androidFormat,
        casing = Casing.SnakeCase,
    )

    val ios by createOption(
        prefix = "${prefix}Ios",
        defaultFormat = iosFormat,
        casing = Casing.UpperCamelCase,
    )

    val web by createOption(
        prefix = "${prefix}Web",
        defaultFormat = webFormat,
        casing = Casing.SnakeCase,
    )

    companion object {
        private fun OptionGroup.createOption(
            prefix: String,
            defaultFormat: String,
            casing: Casing,
        ) = option(
            "--${prefix}Format",
            help = "Naming format for ${prefix}. Supported tokens: ${NodeTokenStringGenerator.tokens.joinToString(",") { it.tokenFormat }}",
        )
            .convert { NodeTokenStringGenerator(it, casing) }
            .default(NodeTokenStringGenerator(defaultFormat, casing))
    }
}
