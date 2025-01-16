package com.anifichadia.figstract.cli.core

import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.http.URLBuilder

enum class ProxyType {
    HTTP,
    SOCKS,
    NONE,
}

@Throws(IllegalArgumentException::class)
fun ProxyType.toProxyConfig(host: String?, port: Int?): ProxyConfig? {
    return when (this) {
        ProxyType.HTTP -> {
            requireNotNull(host) { "host must be specified for HTTP proxy" }

            val url = URLBuilder(host)
                .apply {
                    if (port != null) {
                        this.port = port
                    }
                }
                .build()
            ProxyBuilder.http(url)
        }

        ProxyType.SOCKS -> {
            requireNotNull(host) { "host must be specified for SOCKS proxy" }
            requireNotNull(port) { "port must be specified for SOCKS proxy" }

            ProxyBuilder.socks(host, port)
        }

        ProxyType.NONE -> {
            null
        }
    }
}
