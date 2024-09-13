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
    private val throttleDelayDurations: List<Duration> = DEFAULT_THROTTLE_DELAY_DURATIONS,
) : FigmaApi {
    private val concurrencySemaphore = Semaphore(concurrencyLimit)
    private val floodMitigationMutex = Mutex()

    override suspend fun getFile(
        key: FileKey,
    ): ApiResponse<GetFilesResponse> = wrapRequest {
        getFile(key = key)
    }

    override suspend fun getImages(
        key: FileKey,
        ids: List<String>,
        format: ExportSetting.Format,
        scale: Float,
        contentsOnly: Boolean?,
    ): ApiResponse<GetImageResponse> = wrapRequest {
        getImages(
            key = key,
            ids = ids,
            format = format,
            scale = scale,
            contentsOnly = contentsOnly,
        )
    }

    override suspend fun getLocalVariables(
        key: FileKey,
    ): ApiResponse<GetLocalVariablesResponse> = wrapRequest {
        getLocalVariables(
            key = key,
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
                        delay(throttleDelayDurations[(attempt - 1).coerceIn(throttleDelayDurations.indices)])
                    }
                }

                attempt += 1
            }

            return lastApiResponse
        }
    }

    companion object {
        const val DEFAULT_CONCURRENCY_LIMIT = 5
        const val DEFAULT_RETRY_LIMIT = 5

        val DEFAULT_THROTTLE_DELAY_DURATIONS: List<Duration> = listOf(
            1.seconds,
            5.seconds,
            15.seconds,
            30.seconds,
            1.minutes,
        )
    }
}
