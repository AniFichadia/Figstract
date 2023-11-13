package com.anifichadia.figmaimporter

import com.anifichadia.figmaimporter.type.serializer.OffsetDateTimeSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object HttpClientFactory {
    fun figma(
        timeout: Duration = 120.seconds,
        proxy: ProxyConfig? = null,
        configureJson: JsonBuilder.() -> Unit = {},
    ): HttpClient {
        return HttpClient(CIO) {
            expectSuccess = true

            engine {
                this.proxy = proxy
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        isLenient = false
                        ignoreUnknownKeys = true
                        allowSpecialFloatingPointValues = true
                        useArrayPolymorphism = false

                        configureJson(this)

                        serializersModule = SerializersModule {
                            contextual(OffsetDateTimeSerializer())
                        }
                    }
                )
            }
            install(HttpTimeout) {
                val timeoutMillis = timeout.inWholeMilliseconds
                requestTimeoutMillis = timeoutMillis
                connectTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }
            install(HttpCache) {
                val storage = CacheStorage.Unlimited()
                publicStorage(storage)
                privateStorage(storage)
            }

            BrowserUserAgent()
        }
    }

    fun downloader(
        timeout: Duration = 120.seconds,
        proxy: ProxyConfig? = null,
    ): HttpClient {
        return HttpClient(CIO) {
            expectSuccess = true

            engine {
                this.proxy = proxy
            }

            install(HttpTimeout) {
                val timeoutMillis = timeout.inWholeMilliseconds
                requestTimeoutMillis = timeoutMillis
                connectTimeoutMillis = timeoutMillis
                socketTimeoutMillis = timeoutMillis
            }

            BrowserUserAgent()
        }
    }
}
