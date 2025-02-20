package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource
import java.io.File

fun BaseCliktCommand<*>.defaultPropertyValueSource(
    file: File = File("$commandName.properties"),
): ValueSource {
    return PropertiesValueSource.from(
        file = file,
        getKey = ValueSource.nameOnlyKey(),
    )
}

fun BaseCliktCommand<*>.defaultJsonValueSource(
    file: File = File("$commandName.json"),
): ValueSource {
    return JsonValueSource.from(
        file = file,
        getKey = ValueSource.nameOnlyKey(),
    )
}

fun ValueSource.Companion.nameOnlyKey(): (Context, Option) -> String = { _, option ->
    // Just uses the option name without leading hyphens
    name(option).replace("-", "")
}
