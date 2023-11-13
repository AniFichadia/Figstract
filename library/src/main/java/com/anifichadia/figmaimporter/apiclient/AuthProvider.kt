package com.anifichadia.figmaimporter.apiclient

import io.ktor.client.request.*

sealed interface AuthProvider {
    fun decorateRequest(request: HttpRequestBuilder)

    data class AuthToken(val token: String) : AuthProvider {
        override fun decorateRequest(request: HttpRequestBuilder) {
            request.header("X-Figma-Token", token)
        }
    }

    // TODO: future
//    class OAuth2 : Auth
}
