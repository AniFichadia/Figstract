package com.anifichadia.figmaimporter.cli.core

import kotlinx.cli.ArgType

object BooleanChoice : ArgType<Boolean>(true) {
    private val delegate = Choice(
        listOf(true, false),
        toVariant = { it.toBooleanStrict() },
    )

    override val description = delegate.description

    override fun convert(value: kotlin.String, name: kotlin.String): kotlin.Boolean {
        return delegate.convert(value, name)
    }
}

val ArgType.Companion.BooleanChoice: BooleanChoice
    get() = com.anifichadia.figmaimporter.cli.core.BooleanChoice
