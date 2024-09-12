package com.anifichadia.figstract.cli.core

import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

fun getProxyConfig(proxyHost: String?, proxyPort: Int?): ProxyConfig? {
    return if (proxyHost != null) {
        ProxyBuilder.http(
            URLBuilder(
                protocol = URLProtocol.HTTP,
                host = proxyHost,
            ).apply {
                if (proxyPort != null) {
                    this.port = proxyPort
                }
            }.build()
        )
    } else {
        null
    }
}
