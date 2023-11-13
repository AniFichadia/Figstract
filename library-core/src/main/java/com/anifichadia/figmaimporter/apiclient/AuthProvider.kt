package com.anifichadia.figmaimporter.apiclient

import io.ktor.client.request.*

interface AuthProvider {
    fun decorateRequest(request: HttpRequestBuilder)
}
