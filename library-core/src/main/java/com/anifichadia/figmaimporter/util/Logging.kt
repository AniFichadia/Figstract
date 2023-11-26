package com.anifichadia.figmaimporter.util

import io.github.oshai.kotlinlogging.KotlinLogging

fun createLogger(name: String) = KotlinLogging.logger("com.anifichadia.figmaimporter.$name")

fun createStepLogger(name: String) = createLogger("Step.$name")
