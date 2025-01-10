package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.sources.PropertiesValueSource
import com.github.ajalt.clikt.sources.ValueSource
import com.github.ajalt.clikt.sources.ValueSource.Companion.name
import java.io.File

fun BaseCliktCommand<*>.defaultPropertyValueSource(): ValueSource {
    return PropertiesValueSource.from(
        file = File("$commandName.properties"),
        getKey = { _, option ->
            // Just uses the option name without leading hyphens
            name(option).replace("-", "")
        },
    )
}
