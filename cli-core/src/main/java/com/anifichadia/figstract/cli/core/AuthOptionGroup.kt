package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.apiclient.AuthProvider
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum

class AuthOptionGroup : OptionGroup() {
    val authType: AuthType by option("--authType")
        .enum<AuthType>()
        .default(AuthType.AccessToken)
    val authToken: String by option("--auth")
        .required()

    val authProvider: AuthProvider
        get() = authType.createAuthProvider(authToken)
}
