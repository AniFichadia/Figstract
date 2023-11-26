package com.anifichadia.figmaimporter.figma.api

import com.anifichadia.figmaimporter.apiclient.AuthProvider
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

data class AccessToken(val token: String) : AuthProvider {
    override fun decorateRequest(request: HttpRequestBuilder) {
        request.header("X-Figma-Token", token)
    }
}
