package com.anifichadia.figstract.apiclient

import io.ktor.client.request.HttpRequestBuilder

interface AuthProvider {
    fun decorateRequest(request: HttpRequestBuilder)
}
