package com.anifichadia.figmaimporter.figma.api

import com.anifichadia.figmaimporter.apiclient.ApiResponse
import com.anifichadia.figmaimporter.figma.model.Error

object KnownErrors {
    val rateLimitExceeded = Error(429, "Rate limit exceeded")

    suspend fun <R> ApiResponse<R>.errorMatches(error: Error): Boolean {
        return this is ApiResponse.Failure.ResponseError && this.errorBodyAs<Error>() == error
    }
}
