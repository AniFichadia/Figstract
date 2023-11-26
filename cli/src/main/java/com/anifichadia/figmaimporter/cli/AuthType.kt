package com.anifichadia.figmaimporter.cli

import com.anifichadia.figmaimporter.apiclient.AuthProvider
import com.anifichadia.figmaimporter.figma.api.AccessToken

enum class AuthType {
    AccessToken,
    ;
}

fun AuthType.createAuthProvider(authToken: String): AuthProvider {
    return when (this) {
        AuthType.AccessToken -> AccessToken(authToken)
    }
}
