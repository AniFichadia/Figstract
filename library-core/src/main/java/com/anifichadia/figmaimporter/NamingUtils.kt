package com.anifichadia.figmaimporter

fun String.sanitise() = this
    .replace("""[^\w\s]""".toRegex(), "")
    .replace("""\s+""".toRegex(), " ")

@Suppress("FunctionName")
fun String.to_snake_case() = this.lowercase().replace("""\s""".toRegex(), "_")
