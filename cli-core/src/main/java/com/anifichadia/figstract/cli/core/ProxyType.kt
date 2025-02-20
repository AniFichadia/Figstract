package com.anifichadia.figstract.cli.core

import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.http.URLBuilder

enum class ProxyType {
    HTTP,
    SOCKS,
    AUTO,
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

        ProxyType.AUTO -> {
            // Check system properties first
            System.getProperty("https.proxyHost")?.let {
                println("Proxy auto detection: using system property https.proxyHost=$it")
                return ProxyType.HTTP.toProxyConfig(it, System.getProperty("https.proxyPort")?.toInt())
            }
            System.getProperty("http.proxyHost")?.let {
                println("Proxy auto detection: using system property http.proxyHost=$it")
                return ProxyType.HTTP.toProxyConfig(it, System.getProperty("http.proxyPort")?.toInt())
            }

            // Check environment variables
            System.getenv("HTTPS_PROXY")?.let {
                println("Proxy auto detection: using environment variable property HTTPS_PROXY=$it")
                return ProxyType.HTTP.toProxyConfig(it, null)
            }
            System.getenv("HTTP_PROXY")?.let {
                println("Proxy auto detection: using environment variable property HTTP_PROXY=$it")
                return ProxyType.HTTP.toProxyConfig(it, null)
            }

            println("Proxy auto detection: no proxy detected")
            null
        }

        ProxyType.NONE -> {
            null
        }
    }
}
