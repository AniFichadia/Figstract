package com.anifichadia.figstract.cli.core

import ch.qos.logback.classic.Level

enum class LogLevel {
    OFF,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE,
    ;
}

fun LogLevel.toLogbackLogLevel() = when (this) {
    LogLevel.OFF -> Level.OFF
    LogLevel.ERROR -> Level.ERROR
    LogLevel.WARN -> Level.WARN
    LogLevel.INFO -> Level.INFO
    LogLevel.DEBUG -> Level.DEBUG
    LogLevel.TRACE -> Level.TRACE
}
