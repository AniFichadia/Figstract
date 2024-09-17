package com.anifichadia.figstract.model

import com.anifichadia.figstract.util.TO_SCREAMING_SNAKE_CASE
import com.anifichadia.figstract.util.ToUpperCamelCase
import com.anifichadia.figstract.util.sanitise
import com.anifichadia.figstract.util.toLowerCamelCase
import com.anifichadia.figstract.util.to_snake_case

abstract class TokenStringGenerator<T> {
    abstract val tokens: List<Token<T>>
    abstract val format: String
    abstract val casing: Casing

    fun generate(from: T, prefix: String? = null, suffix: String? = null): String {
        val tokenised = tokens.fold(format) { name, token ->
            token.update(name, from)
        }

        return listOfNotNull(prefix, tokenised, suffix)
            .map { casing.transform(it) }
            .let { casing.concat(it) }
            .sanitise()
    }

    enum class Casing {
        None {
            override val separator: String = ""

            override fun transform(string: String): String = string

            override fun concat(first: String, second: String): String = "${first}${separator}${second}"
        },
        UpperCamelCase {
            override val separator: String = ""

            override fun transform(string: String): String = string.ToUpperCamelCase()

            override fun concat(first: String, second: String): String =
                "${first}${separator}${second.replaceFirstChar { it.uppercase() }}"
        },
        LowerCamelCase {
            override val separator: String = ""

            override fun transform(string: String): String = string.toLowerCamelCase()

            override fun concat(first: String, second: String): String =
                "${first}${separator}${second.replaceFirstChar { it.lowercase() }}"
        },
        SnakeCase {
            override val separator: String = "_"

            override fun transform(string: String): String = string.to_snake_case()

            override fun concat(first: String, second: String): String =
                "${first}${separator}${second.replaceFirstChar { it.lowercase() }}"
        },
        ScreamingSnakeCase {
            override val separator: String = "_"

            override fun transform(string: String): String = string.TO_SCREAMING_SNAKE_CASE()

            override fun concat(first: String, second: String): String =
                "${first}${separator}${second.replaceFirstChar { it.uppercase() }}"
        },
        ;

        abstract val separator: String

        abstract fun transform(string: String): String

        abstract fun concat(first: String, second: String): String

        fun concat(strings: List<String>): String {
            return when {
                strings.isEmpty() -> ""
                strings.size == 1 -> strings.first()
                else -> strings.fold("") { acc, s ->
                    when {
                        acc.isEmpty() -> s
                        s.isEmpty() -> acc
                        else -> concat(acc, s)
                    }
                }
            }
        }
    }

    sealed class Token<T>(
        contentPattern: String,
    ) {
        val format: Regex = """\{${contentPattern}}""".toRegex()

        abstract fun extractValue(matchResult: MatchResult, value: T): String?

        fun update(string: String, value: T): String {
            return format.replace(string) {
                val extractValue = extractValue(it, value)
                extractValue ?: it.value
            }
        }

        data class Simple<T>(
            val name: String,
            val extractValue: (T) -> String?,
        ) : Token<T>(name) {
            override fun extractValue(matchResult: MatchResult, value: T): String? = extractValue(value)
        }

        data class Complex<T>(
            private val contentFormat: Regex,
            val doExtractValue: (matchResult: MatchResult, value: T) -> String?,
        ) : Token<T>(contentFormat.pattern) {
            override fun extractValue(matchResult: MatchResult, value: T): String? = doExtractValue(matchResult, value)
        }
    }
}
