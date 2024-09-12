package com.anifichadia.figstract.figma.api

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.model.Error

object KnownErrors {
    val rateLimitExceeded = Error(429, "Rate limit exceeded")

    suspend fun <R> ApiResponse<R>.errorMatches(error: Error): Boolean {
        return if (this is ApiResponse.Failure.ResponseError) {
            runCatching {
                this.errorBodyAsOrNull<Error>() == error
            }.getOrElse { false }
        } else {
            false
        }
    }
}
