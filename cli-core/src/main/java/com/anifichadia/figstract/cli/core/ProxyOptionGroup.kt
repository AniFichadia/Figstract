package com.anifichadia.figstract.cli.core

import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import io.ktor.client.engine.ProxyConfig

class ProxyOptionGroup : OptionGroup() {
    val proxyHost: String? by option("--proxyHost")
    val proxyPort: Int? by option("--proxyPort")
        .int()

    val proxyConfig: ProxyConfig?
        get() = getProxyConfig(proxyHost, proxyPort)
}
