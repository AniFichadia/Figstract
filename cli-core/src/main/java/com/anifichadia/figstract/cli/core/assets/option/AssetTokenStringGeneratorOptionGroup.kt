package com.anifichadia.figstract.cli.core.assets.option

import com.anifichadia.figstract.cli.core.DelegatableOptionGroup
import com.anifichadia.figstract.cli.core.assets.model.NodeTokenStringGenerator
import com.anifichadia.figstract.model.TokenStringGenerator
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.OptionWithValues
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
        casing = TokenStringGenerator.Casing.SnakeCase,
    )

    val ios by createOption(
        prefix = "${prefix}Ios",
        defaultFormat = iosFormat,
        casing = TokenStringGenerator.Casing.UpperCamelCase,
    )

    val web by createOption(
        prefix = "${prefix}Web",
        defaultFormat = webFormat,
        casing = TokenStringGenerator.Casing.SnakeCase,
    )

    companion object {
        fun OptionGroup.createOption(
            prefix: String,
            defaultFormat: String,
            casing: TokenStringGenerator.Casing,
        ): OptionWithValues<NodeTokenStringGenerator, NodeTokenStringGenerator, NodeTokenStringGenerator> {
            val tokens = NodeTokenStringGenerator.tokens.joinToString(", ") { it.format.pattern }
            return option(
                "--${prefix}NamingFormat",
                help = "Naming format for ${prefix}. Supported tokens: $tokens",
            )
                .convert { NodeTokenStringGenerator(it, casing) }
                .default(NodeTokenStringGenerator(defaultFormat, casing))
        }
    }
}
