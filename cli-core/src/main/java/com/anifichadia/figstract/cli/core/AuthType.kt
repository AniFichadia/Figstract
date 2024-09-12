package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.apiclient.AuthProvider
import com.anifichadia.figstract.figma.api.AccessToken

enum class AuthType {
    AccessToken,
    ;
}

fun AuthType.createAuthProvider(authToken: String): AuthProvider {
    return when (this) {
        AuthType.AccessToken -> AccessToken(authToken)
    }
}
