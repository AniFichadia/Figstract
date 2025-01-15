package com.anifichadia.figstract.cli.core

import com.anifichadia.figstract.apiclient.AuthProvider
import com.anifichadia.figstract.figma.api.FigmaApi
import com.github.ajalt.clikt.core.BaseCliktCommand
import com.github.ajalt.clikt.core.findObject
import com.github.ajalt.clikt.core.requireObject
import io.ktor.client.engine.ProxyConfig
import java.net.Proxy

//region AuthProvider
private const val KEY_AUTH_PROVIDER = "AuthProvider"

fun BaseCliktCommand<*>.setAuthProvider(authProvider: AuthProvider) {
    currentContext.findOrSetObject<AuthProvider>(KEY_AUTH_PROVIDER) { authProvider }
}

fun BaseCliktCommand<*>.getAuthProvider() = requireObject<AuthProvider>(KEY_AUTH_PROVIDER)
//endregion

//region ProxyConfig
private const val KEY_PROXY_CONFIG = "ProxyConfig"

fun BaseCliktCommand<*>.setProxy(proxyConfig: Proxy?) {
    if (proxyConfig != null) {
        currentContext.findOrSetObject<ProxyConfig>(KEY_PROXY_CONFIG) { proxyConfig }
    }
}

fun BaseCliktCommand<*>.getProxy() = findObject<ProxyConfig>(KEY_PROXY_CONFIG)
//endregion

//region FigmaApi
private const val KEY_FIGMA_API = "FigmaApi"

fun BaseCliktCommand<*>.setFigmaApi(figmaApi: FigmaApi) {
    currentContext.findOrSetObject<FigmaApi>(KEY_FIGMA_API) { figmaApi }
}

fun BaseCliktCommand<*>.getFigmaApi() = requireObject<FigmaApi>(KEY_FIGMA_API)
//endregion
