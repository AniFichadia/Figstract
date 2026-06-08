package com.anifichadia.figstract.figma.api

import com.anifichadia.figstract.apiclient.ApiResponse
import com.anifichadia.figstract.figma.FileKey
import com.anifichadia.figstract.figma.api.KnownErrors.errorMatches
import com.anifichadia.figstract.figma.model.ExportSetting
import com.anifichadia.figstract.figma.model.GetFilesResponse
import com.anifichadia.figstract.figma.model.GetImageResponse
import com.anifichadia.figstract.figma.model.GetLocalVariablesResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlin.math.pow
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Figma's API may rate limit requests.
 *
 * This prevents or reduces the impact of Figma API rate limiting failures by limiting the concurrency of requests to
 * Figma and using a progressive backoff as soon as a rate limiting error occurs.
 *
 * So we trade off speed for reliability.
 */
class FigmaApiProxyWithFlowControl(
    private val wrapped: FigmaApi,
    concurrencyLimit: Int = DEFAULT_CONCURRENCY_LIMIT,
    private val retryLimit: Int = DEFAULT_RETRY_LIMIT,
    private val baseDelay: Duration = DEFAULT_BASE_DELAY,
    private val maxDelay: Duration = DEFAULT_MAX_DELAY,
) : FigmaApi {
    private val concurrencySemaphore = Semaphore(concurrencyLimit)
    private val floodMitigationMutex = Mutex()

    override suspend fun getFile(
        key: FileKey,
        branchData: Boolean?,
        version: String?,
    ): ApiResponse<GetFilesResponse> = wrapRequest {
        getFile(
            key = key,
            branchData = branchData,
            version = version,
        )
    }

    override suspend fun getImages(
        key: FileKey,
        ids: List<String>,
        format: ExportSetting.Format,
        scale: Float?,
        contentsOnly: Boolean?,
        useAbsoluteBounds: Boolean?,
    ): ApiResponse<GetImageResponse> {
        val response = wrapRequest {
            getImages(
                key = key,
                ids = ids,
                format = format,
                scale = scale,
                contentsOnly = contentsOnly,
                useAbsoluteBounds = useAbsoluteBounds,
            )
        }

        if (!response.errorMatches(GetImageResponse.tooManyImages, GetImageResponse.tooManyImages2) || ids.size <= 1)
            return response

        // Batch too large and rejected by figma, retry each image individually and merge the results
        val mergedImages = mutableMapOf<String, String?>()
        var lastSuccess: ApiResponse.Success<GetImageResponse>? = null
        for (id in ids) {
            val singleResponse = wrapRequest {
                getImages(
                    key = key,
                    ids = listOf(id),
                    format = format,
                    scale = scale,
                    contentsOnly = contentsOnly,
                    useAbsoluteBounds = useAbsoluteBounds,
                )
            }

            if (singleResponse.isSuccess()) {
                lastSuccess = singleResponse as ApiResponse.Success<GetImageResponse>
                mergedImages.putAll(singleResponse.body.images)
            } else {
                return singleResponse
            }
        }

        return lastSuccess?.copy(
            body = GetImageResponse(images = mergedImages),
        ) ?: ApiResponse.Failure.RequestError(
            exception = IllegalStateException("No successful responses when retrying images individually for key: $key"),
        )
    }

    override suspend fun getLocalVariables(
        key: FileKey,
        version: String?,
    ): ApiResponse<GetLocalVariablesResponse> = wrapRequest {
        getLocalVariables(
            key = key,
            version = version,
        )
    }

    private suspend inline fun <V> wrapRequest(block: FigmaApi.() -> ApiResponse<V>): ApiResponse<V> {
        concurrencySemaphore.withPermit {
            var attempt = 1
            lateinit var lastApiResponse: ApiResponse<V>
            while (attempt <= retryLimit) {
                floodMitigationMutex.withLock {
                    // Just await the mutex unlocking
                }

                lastApiResponse = wrapped.block()

                val isRateLimitError = lastApiResponse.errorMatches(KnownErrors.rateLimitExceeded)
                if (!isRateLimitError) {
                    return lastApiResponse
                } else if (!floodMitigationMutex.isLocked) {
                    floodMitigationMutex.withLock {
                        delay(throttleDelay(attempt))
                    }
                }

                attempt += 1
            }

            return lastApiResponse
        }
    }

    private fun throttleDelay(attempt: Int): Duration {
        val exponential = minOf(maxDelay, baseDelay * 2.0.pow(attempt))
        return exponential * Random.nextDouble(0.5, 1.0)
    }

    companion object {
        const val DEFAULT_CONCURRENCY_LIMIT = 5
        const val DEFAULT_RETRY_LIMIT = 15
        val DEFAULT_BASE_DELAY = 1.seconds
        val DEFAULT_MAX_DELAY = 2.minutes
    }
}
