package com.anifichadia.figstract.figma.api

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.model.Error

object KnownErrors {
    val rateLimitExceeded = Error(429, "Rate limit exceeded")

    suspend fun <R> ApiResponse<R>.errorMatches(error: Error, vararg errors: Error): Boolean {
        val allErrors = listOf(error, *errors)
        return if (this is ApiResponse.Failure.ResponseError) {
            runCatching {
                val errorBody = this.errorBodyAsOrNull<Error>()
                allErrors.any { error -> errorBody == error }
            }.getOrElse { false }
        } else {
            false
        }
    }
}
