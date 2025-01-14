package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum

fun <T : BaseCliktCommand<T>> BaseCliktCommand<T>.logLevel(): OptionWithValues<LogLevel, LogLevel, LogLevel> {
    return option("--logLevel")
        .enum<LogLevel>(ignoreCase = true)
        .default(LogLevel.ERROR)
}
