package com.asterixorobelix.remit.data.network

import com.asterixorobelix.remit.platform.Logger
import com.asterixorobelix.remit.platform.TaggedLogger
import com.asterixorobelix.remit.platform.withTag
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Configuration for the [RetryPlugin].
 */
class RetryPluginConfig {
    /**
     * The retry policy defining max retries, delays, and jitter.
     */
    var retryPolicy: RetryPolicy = RetryPolicy.DEFAULT

    /**
     * HTTP status codes that should trigger a retry.
     * Default includes common transient error codes.
     */
    var retryableStatusCodes: Set<HttpStatusCode> = DEFAULT_RETRYABLE_STATUS_CODES

    /**
     * Custom predicate to determine if a response should be retried.
     * This is evaluated in addition to [retryableStatusCodes].
     */
    var shouldRetry: ((HttpResponse) -> Boolean)? = null

    /**
     * Optional logger for debugging retry attempts.
     */
    var logger: Logger? = null

    companion object {
        /**
         * Default HTTP status codes that warrant a retry:
         * - 408 Request Timeout
         * - 429 Too Many Requests
         * - 500 Internal Server Error
         * - 502 Bad Gateway
         * - 503 Service Unavailable
         * - 504 Gateway Timeout
         */
        val DEFAULT_RETRYABLE_STATUS_CODES: Set<HttpStatusCode> = setOf(
            HttpStatusCode.RequestTimeout, // 408
            HttpStatusCode.TooManyRequests, // 429
            HttpStatusCode.InternalServerError, // 500
            HttpStatusCode.BadGateway, // 502
            HttpStatusCode.ServiceUnavailable, // 503
            HttpStatusCode.GatewayTimeout, // 504
        )
    }
}

/**
 * A Ktor HttpClient plugin that implements retry with exponential backoff and jitter.
 *
 * This plugin intercepts failed requests and automatically retries them based on
 * configurable criteria. It respects coroutine cancellation and uses the
 * [RetryPolicy] for delay calculations.
 *
 * Features:
 * - Exponential backoff with jitter to prevent thundering herd
 * - Configurable retryable status codes
 * - Custom shouldRetry predicate for advanced scenarios
 * - Proper cancellation handling via [kotlinx.coroutines.isActive]
 * - Optional logging for debugging
 *
 * Usage:
 * ```
 * val client = HttpClient {
 *     install(RetryPlugin) {
 *         retryPolicy = RetryPolicy(maxRetries = 5)
 *         retryableStatusCodes = setOf(HttpStatusCode.ServiceUnavailable)
 *         logger = myLogger
 *     }
 * }
 * ```
 *
 * The plugin will NOT retry:
 * - Non-idempotent requests (POST/PATCH) unless marked with [allowRetry]. Replaying a POST can
 *   duplicate side effects, and replaying `/auth/refresh` would reuse a rotated refresh token.
 * - 4xx client errors (except 408 and 429)
 * - [CancellationException] (rethrown immediately)
 * - Responses that don't match retryable criteria
 */
val RetryPlugin = createClientPlugin("RetryPlugin", ::RetryPluginConfig) {
    val config = pluginConfig
    val policy = config.retryPolicy
    val retryableCodes = config.retryableStatusCodes
    val customShouldRetry = config.shouldRetry
    val log: TaggedLogger? = config.logger?.withTag("RetryPlugin")

    on(Send) { request ->
        if (!request.isRetryable()) return@on proceed(request)

        var attempt = 0
        var lastCall: HttpClientCall? = null
        var lastException: Throwable? = null

        while (attempt <= policy.maxRetries) {
            // Check for cancellation before each attempt
            if (!coroutineContext.isActive) {
                log?.d("Request cancelled before attempt ${attempt + 1}")
                throw CancellationException("Request was cancelled")
            }

            try {
                val call = proceed(request)
                lastCall = call
                val responseStatus = call.response.status

                // Check if response is successful
                if (responseStatus.isSuccess()) {
                    if (attempt > 0) {
                        log?.i("Request succeeded after $attempt retries")
                    }
                    return@on call
                }

                // Check if we should retry this response
                val isRetryable = responseStatus in retryableCodes ||
                    (customShouldRetry?.invoke(call.response) == true)

                if (!isRetryable) {
                    log?.d("Response $responseStatus is not retryable")
                    return@on call
                }

                // Check if we have retries left
                if (attempt >= policy.maxRetries) {
                    log?.w("Max retries (${policy.maxRetries}) exhausted, returning last response")
                    return@on call
                }

                // Calculate delay and wait
                val delayMs = policy.calculateDelay(attempt)
                log?.d("Attempt ${attempt + 1} failed with $responseStatus, retrying in ${delayMs}ms")

                // Use kotlinx.coroutines.delay which respects cancellation
                delay(delayMs)
                attempt++
            } catch (e: CancellationException) {
                // Always rethrow cancellation exceptions immediately
                log?.d("Request cancelled during attempt ${attempt + 1}")
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                // Intentionally catching generic Exception - retry interceptor should retry
                // on any exception type (IOException, ConnectException, UnknownHostException, etc.)
                lastException = e

                // Check if we should retry on exception
                if (attempt >= policy.maxRetries) {
                    log?.e("Max retries exhausted after exception: ${e.message}", e)
                    throw e
                }

                // Calculate delay and wait before retrying
                val delayMs = policy.calculateDelay(attempt)
                log?.d("Attempt ${attempt + 1} failed with exception: ${e.message}, retrying in ${delayMs}ms")

                // Check for cancellation before delay
                if (!coroutineContext.isActive) {
                    throw CancellationException("Request was cancelled")
                }

                delay(delayMs)
                attempt++
            }
        }

        // This should not be reached, but handle edge cases
        lastCall?.let { return@on it }
        lastException?.let { throw it }

        // Fallback: shouldn't happen but proceed with original request
        proceed(request)
    }
}

private val RetryNonIdempotentKey = AttributeKey<Unit>("RetryNonIdempotent")
private val IDEMPOTENT_METHODS = setOf(
    HttpMethod.Get,
    HttpMethod.Head,
    HttpMethod.Put,
    HttpMethod.Delete,
    HttpMethod.Options,
)

/** Opts a non-idempotent request (for example a POST that is safe to replay) into retries. */
fun HttpRequestBuilder.allowRetry() {
    attributes.put(RetryNonIdempotentKey, Unit)
}

internal fun HttpRequestBuilder.isRetryable(): Boolean =
    method in IDEMPOTENT_METHODS || attributes.contains(RetryNonIdempotentKey)
