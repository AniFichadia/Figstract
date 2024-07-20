package com.anifichadia.figmaimporter.apiclient

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.URLBuilder

data class Endpoint(
    val scheme: String,
    val host: String,
    val port: Int? = null,
) {
    val baseUrl: String = buildString {
        append(scheme)
        append("://")
        append(host)
        port?.let { port ->
            append(":")
            append(port)
        }
    }

    val isSecure = scheme.equals("https", ignoreCase = true)
}

val localhostEndpoint = Endpoint(
    scheme = "http",
    host = "localhost",
    port = 8080,
)

fun HttpRequestBuilder.url(endpoint: Endpoint, block: URLBuilder.() -> Unit) {
    this@url.url(
        scheme = endpoint.scheme,
        host = endpoint.host,
        port = endpoint.port,
        block = block,
    )
}
