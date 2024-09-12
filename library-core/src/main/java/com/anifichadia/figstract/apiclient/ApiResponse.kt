package com.anifichadia.figstract.apiclient

import io.ktor.client.statement.HttpResponse
import io.ktor.util.reflect.typeInfo
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
sealed class ApiResponse<ValueT> {
    data class Success<ValueT>(
        val statusCode: Int,
        val response: HttpResponse,
        val body: ValueT,
        val bodyText: String,
        val metaData: MetaData,
    ) : ApiResponse<ValueT>()

    sealed class Failure<ValueT> : ApiResponse<ValueT>() {
        abstract fun asException(): Throwable

        data class ResponseError<ValueT>(
            val statusCode: Int,
            val response: HttpResponse,
            val errorBodyString: String,
            val metaData: MetaData,
        ) : Failure<ValueT>() {
            suspend inline fun <reified T> errorBodyAs() = requireNotNull(errorBodyAsOrNull<T>())

            suspend inline fun <reified T> errorBodyAsOrNull() = response.call.bodyNullable(typeInfo<T>())

            override fun asException() = ResponseException(this)

            class ResponseException(
                val error: ResponseError<*>,
            ) : Exception(error.toString())
        }

        data class RequestError<ValueT>(
            val exception: Throwable,
        ) : Failure<ValueT>() {
            override fun asException() = exception
        }
    }

    data class MetaData(
        val callDuration: Long,
    )


    fun isSuccess(): Boolean {
        contract {
            returns(true) implies (this@ApiResponse is Success<ValueT>)
            returns(false) implies (this@ApiResponse is Failure<ValueT>)
        }

        return this is Success
    }

    fun isFailure(): Boolean {
        contract {
            returns(true) implies (this@ApiResponse is Failure<ValueT>)
            returns(false) implies (this@ApiResponse is Success<ValueT>)
        }

        return this is Failure
    }

    inline fun <ReturnT> handle(
        success: (Success<ValueT>) -> ReturnT,
        failure: (Failure<ValueT>) -> ReturnT,
    ): ReturnT {
        contract {
            callsInPlace(success, InvocationKind.AT_MOST_ONCE)
            callsInPlace(failure, InvocationKind.AT_MOST_ONCE)
        }

        return if (this.isSuccess()) {
            success(this)
        } else {
            failure(this)
        }
    }

    @Throws(Throwable::class)
    fun successOrThrow() {
        contract {
            returns() implies (this@ApiResponse is Success<ValueT>)
        }

        if (this.isFailure()) {
            throw this.asException()
        }
    }

    @Throws(Throwable::class)
    fun successBodyOrThrow(): ValueT {
        contract {
            returns() implies (this@ApiResponse is Success<ValueT>)
        }

        return handle(
            success = { value -> value.body },
            failure = { failure -> throw failure.asException() },
        )
    }

    fun successBodyOrFallback(fallbackLazy: () -> ValueT): ValueT {
        contract {
            callsInPlace(fallbackLazy, InvocationKind.AT_MOST_ONCE)
        }

        return handle(
            success = { value -> value.body },
            failure = { fallbackLazy() },
        )
    }

    fun successBodyOrNull(): ValueT? = handle(
        success = { value -> value.body },
        failure = { null },
    )

    inline fun onSuccess(action: Success<ValueT>.() -> Unit): ApiResponse<ValueT> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }

        if (this.isSuccess()) {
            action(this)
        }

        return this
    }

    inline fun onFailure(action: Failure<ValueT>.() -> Unit): ApiResponse<ValueT> {
        contract {
            callsInPlace(action, InvocationKind.AT_MOST_ONCE)
        }

        if (this.isFailure()) {
            action(this)
        }

        return this
    }
}
