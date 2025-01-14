package com.anifichadia.figstract.cli.core

import ch.qos.logback.classic.Logger
import com.anifichadia.figstract.util.createLogger
import org.slf4j.LoggerFactory

val timingLogger = createLogger("Timing")

fun getRootLogger(): Logger {
    return LoggerFactory.getILoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME) as Logger
}
