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

    fun generate(from: T): String {
        val tokenised = tokens.fold(format) { name, token ->
            token.update(name, from)
        }
        val sanitised = tokenised.sanitise()
        val cased = casing.transform(sanitised)

        return cased
    }

    enum class Casing {
        None {
            override fun transform(string: String): String = string
        },
        UpperCamelCase {
            override fun transform(string: String): String = string.ToUpperCamelCase()
        },
        LowerCamelCase {
            override fun transform(string: String): String = string.toLowerCamelCase()
        },
        SnakeCase {
            override fun transform(string: String): String = string.to_snake_case()
        },
        ScreamingSnakeCase {
            override fun transform(string: String): String = string.TO_SCREAMING_SNAKE_CASE()
        },
        ;

        abstract fun transform(string: String): String
    }

    data class Token<T>(
        val name: String,
        val extractValue: (T) -> String?,
    ) {
        val tokenFormat = "{$name}"

        fun update(string: String, value: T): String {
            val extracted = extractValue(value)
            return if (extracted != null) {
                string.replace(tokenFormat, extracted)
            } else {
                string
            }
        }
    }
}
