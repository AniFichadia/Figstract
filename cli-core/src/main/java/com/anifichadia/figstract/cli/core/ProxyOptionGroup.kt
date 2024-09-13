package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.ProxyType

class ProxyOptionGroup : OptionGroup() {
    val type: ProxyType by option("--proxyType")
        .enum<ProxyType>(ignoreCase = true)
        .default(ProxyType.UNKNOWN)
    val host: String? by option("--proxyHost")
    val port: Int? by option("--proxyPort")
        .int()

    val proxyConfig: ProxyConfig?
        @Throws(IllegalArgumentException::class)
        get() = type.toProxyConfig(host, port)
}
