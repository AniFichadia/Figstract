package com.anifichadia.figstract.apiclient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractApiClient(
    val httpClient: HttpClient,
    val endpoint: Endpoint,
    val basePath: String? = null,
    val authProvider: AuthProvider? = null,
) {
    protected suspend inline fun <reified T> apiRequest(
        method: HttpMethod,
        path: String,
        queryParams: Map<String, Any?> = emptyMap(),
        authenticated: Boolean,
        authProvider: AuthProvider? = null,
    ): ApiResponse<T> = apiRequest<Any?, T>(
        method = method,
        path = path,
        queryParams = queryParams,
        authenticated = authenticated,
        authProvider = authProvider,
        body = null,
    )

    protected suspend inline fun <reified B, reified T> apiRequest(
        method: HttpMethod,
        path: String,
        queryParams: Map<String, Any?> = emptyMap(),
        authenticated: Boolean,
        authProvider: AuthProvider? = null,
        body: B?,
    ): ApiResponse<T> {
        return try {
            val response = request(method, path, queryParams, authenticated, authProvider, body)
            val bodyText = response.bodyAsText()
            val responseBody = response.body<T>()
            val metaData = createMetaData(response)

            ApiResponse.Success(
                statusCode = response.status.value,
                response = response,
                body = responseBody,
                bodyText = bodyText,
                metaData = metaData,
            )
        } catch (e: ResponseException) {
            try {
                val response = e.response
                val errorBodyString = response.body<String>()
                val metaData = createMetaData(response)

                ApiResponse.Failure.ResponseError(
                    statusCode = response.status.value,
                    response = response,
                    errorBodyString = errorBodyString,
                    metaData = metaData,
                )
            } catch (e: Throwable) {
                ApiResponse.Failure.RequestError(e)
            }
        } catch (e: Throwable) {
            ApiResponse.Failure.RequestError(e)
        }
    }

    protected suspend inline fun <reified B : Any> request(
        method: HttpMethod,
        path: String,
        queryParams: Map<String, Any?> = emptyMap(),
        authenticated: Boolean = true,
        authProvider: AuthProvider? = null,
        body: B? = null,
    ): HttpResponse {
        val actualAuthProvider = authProvider ?: this.authProvider

        require((authenticated && actualAuthProvider != null) || !authenticated) { "authProvider must be supplied for authenticated calls" }

        val request = httpClient.prepareRequest {
            this.method = method

            this.url(endpoint) {
                appendPathSegments(
                    *pathSegments(basePath, path)
                        .sanitise()
                        .toTypedArray()
                )
            }

            queryParams
                .entries
                .filter { (_, value) -> value != null }
                .forEach { (key, value) ->
                    when (value) {
                        is Iterable<*> -> value.forEach { parameter(key, it) }
                        is Array<*> -> value.forEach { parameter(key, it) }
                        else -> parameter(key, value)
                    }
                }

            if (authenticated && actualAuthProvider != null) {
                actualAuthProvider.decorateRequest(this)
            }

            if (body != null) {
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                setBody(body)
            }
        }

        val response = request.execute()

        return response
    }

    protected fun createMetaData(response: HttpResponse): ApiResponse.MetaData {
        val requestTimestamp = response.requestTime.timestamp
        val responseTimestamp = response.responseTime.timestamp

        return ApiResponse.MetaData(
            callDuration = responseTimestamp - requestTimestamp
        )
    }
}
