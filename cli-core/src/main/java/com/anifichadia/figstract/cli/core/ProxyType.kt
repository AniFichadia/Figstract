package com.anifichadia.figstract.cli.core

import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.ProxyType
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

@Throws(IllegalArgumentException::class)
fun ProxyType.toProxyConfig(host: String?, port: Int?): ProxyConfig? {
    return when (this) {
        ProxyType.SOCKS -> {
            when {
                host == null -> throw IllegalArgumentException("host must be specified for SOCKS proxy")
                port == null -> throw IllegalArgumentException("port must be specified for SOCKS proxy")
                else -> ProxyBuilder.socks(host, port)
            }
        }

        ProxyType.HTTP -> {
            if (host == null) throw IllegalArgumentException("host must be specified for HTTP proxy")

            ProxyBuilder.http(
                URLBuilder(
                    protocol = URLProtocol.HTTP,
                    host = host,
                    port = port ?: DEFAULT_PORT,
                ).build()
            )
        }

        ProxyType.UNKNOWN -> {
            null
        }
    }
}
