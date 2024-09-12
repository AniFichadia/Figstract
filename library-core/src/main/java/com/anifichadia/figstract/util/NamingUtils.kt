package com.anifichadia.figstract.util

fun String.sanitise() = this
    .replace('-', ' ')
    .replace("""[^\w\s]""".toRegex(), "")
    .replace("""\s+""".toRegex(), " ")


fun String.sanitiseFileName() = this
    .replace("""[^\w\s]""".toRegex(), " ")
    .replace("""\""", "")
    .replace("""/""", "")
    .replace("""\s+""".toRegex(), " ")

private val spaceRegex = """\s""".toRegex()

@Suppress("FunctionName")
fun String.to_snake_case() = this.lowercase().replace(spaceRegex, "_")

@Suppress("FunctionName")
fun String.TO_SCREAMING_SNAKE_CASE() = this.to_snake_case().uppercase()

@Suppress("FunctionName")
fun String.ToUpperCamelCase(): String = this.toLowerCamelCase().replaceFirstChar { it.uppercase() }

fun String.toLowerCamelCase(): String {
    return this
        .mapIndexed { index, c ->
            val previousIsSpace = if (index > 0) {
                this[index - 1].toString().matches(spaceRegex)
            } else {
                false
            }
            if (previousIsSpace) {
                c.uppercase()
            } else {
                c
            }
        }
        .joinToString("")
        .replace(spaceRegex, "")
        .replaceFirstChar { it.lowercase() }
}
