package com.anifichadia.figstract.util

import io.github.oshai.kotlinlogging.KotlinLogging

fun createLogger(name: String) = KotlinLogging.logger("com.anifichadia.figstract.$name")

fun createStepLogger(name: String) = createLogger("Step.$name")
