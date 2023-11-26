package com.anifichadia.figmaimporter.apiclient

import io.ktor.client.request.HttpRequestBuilder

interface AuthProvider {
    fun decorateRequest(request: HttpRequestBuilder)
}
