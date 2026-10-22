package com.example.myproject.data.network

import com.example.myproject.data.auth.AuthResponse
import com.example.myproject.data.auth.RefreshRequest
import com.example.myproject.data.session.SessionEvents
import com.example.myproject.data.session.SessionTokens
import com.example.myproject.data.session.TokenStore
import com.example.myproject.util.runSuspendCatching
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.RefreshTokensParams
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal val ApiJson = Json {
    ignoreUnknownKeys = true
    isLenient = false
}

/**
 * Builds the HttpClient used for every call to this app's backend.
 *
 * - The base URL is resolved per request, so the debug-only API override applies to the next
 *   request without restarting the app.
 * - Bearer tokens are only attached to requests for the API origin (never to auth endpoints or
 *   third-party hosts); [ApiHostAuthGuard] also strips them from Auth-plugin retries.
 * - Refresh tokens rotate: the rotated pair is persisted compare-and-set, and the session is
 *   cleared only when `/auth/refresh` answers 401/403. Rate limits, server errors and network
 *   failures keep the session so the next request can try again.
 *
 * @param engine Test hook; production uses the platform default engine.
 */
fun createApiHttpClient(
    config: NetworkConfig,
    apiBaseUrl: () -> String,
    tokenStore: TokenStore,
    sessionEvents: SessionEvents,
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
        install(Auth) {
            bearer {
                loadTokens { tokenStore.current()?.toBearer() }
                refreshTokens { refreshSession(tokenStore, sessionEvents) }
                sendWithoutRequest { request ->
                    request.targetsApi(apiBaseUrl()) && !request.isAuthEndpoint()
                }
            }
        }
        install(ApiHostAuthGuard) { this.apiBaseUrl = apiBaseUrl }
        defaultRequest {
            url(apiBaseUrl())
            contentType(ContentType.Application.Json)
        }
    }
    return if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
}

private suspend fun RefreshTokensParams.refreshSession(
    tokenStore: TokenStore,
    sessionEvents: SessionEvents,
): BearerTokens? {
    val sentRefreshToken = tokenStore.refreshToken() ?: return null
    val response = runSuspendCatching {
        client.post(ApiPaths.REFRESH) {
            markAsRefreshTokenRequest()
            setBody(RefreshRequest(sentRefreshToken))
        }
    }.getOrNull() // Offline or timed out: keep the session for the next attempt.

    return when {
        response == null -> null

        response.status.isSuccess() -> storeRotatedTokens(response, sentRefreshToken, tokenStore)

        response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden -> {
            if (tokenStore.clearIfCurrent(sentRefreshToken)) sessionEvents.notifyExpired()
            null
        }

        else -> null // 429 / 5xx: transient, keep the session.
    }
}

private suspend fun storeRotatedTokens(
    response: HttpResponse,
    sentRefreshToken: String,
    tokenStore: TokenStore,
): BearerTokens? {
    val body = runSuspendCatching { response.body<AuthResponse>() }.getOrNull() ?: return null
    val rotated = SessionTokens(body.accessToken, body.refreshToken)
    return if (tokenStore.replaceIfCurrent(sentRefreshToken, rotated)) {
        rotated.toBearer()
    } else {
        // The session changed while refreshing (sign-out or a new sign-in); use it as is.
        tokenStore.current()?.toBearer()
    }
}

private fun SessionTokens.toBearer() = BearerTokens(accessToken, refreshToken)
