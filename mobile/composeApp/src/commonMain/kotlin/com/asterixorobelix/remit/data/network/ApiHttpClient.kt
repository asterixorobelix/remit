package com.asterixorobelix.remit.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val ApiJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
}

/**
 * Builds the HttpClient used for every call to this app's backend.
 *
 * - Every request gets its own `X-Request-ID` ([RequestIdPlugin]).
 * - The base URL is resolved per request, so the debug-only API override applies to the next
 *   request without restarting the app.
 * - Only idempotent methods are retried ([RetryPlugin]); `allowRetry()` opts a POST in.
 *
 * @param engine Test hook; production uses the platform default engine.
 */
fun createApiHttpClient(
    config: NetworkConfig,
    apiBaseUrl: () -> String,
    engine: HttpClientEngine? = null,
): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = config.connectTimeoutMs
            requestTimeoutMillis = config.requestTimeoutMs
            socketTimeoutMillis = config.socketTimeoutMs
        }
        install(RetryPlugin) { retryPolicy = config.retryPolicy }
        install(RequestIdPlugin)
        defaultRequest {
            url(apiBaseUrl())
            contentType(ContentType.Application.Json)
        }
    }
    return if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
}
