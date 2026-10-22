package com.example.myproject.data.network

import com.example.myproject.util.generateId
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.Url
import io.ktor.http.encodedPath

/** Adds a unique `X-Request-ID` to every request (kept identical across retries of that request). */
internal val RequestIdPlugin = createClientPlugin("RequestId") {
    onRequest { request, _ ->
        if (!request.headers.contains(HttpHeaders.XRequestId)) {
            request.headers.append(HttpHeaders.XRequestId, generateId())
        }
    }
}

internal class ApiHostGuardConfig {
    var apiBaseUrl: () -> String = { "" }
}

/**
 * Defense in depth for the bearer token: strips `Authorization` from any request that does not
 * target the configured API origin, including requests retried by the Auth plugin after a 401.
 * Install after `Auth` so it runs closest to the engine.
 */
internal val ApiHostAuthGuard = createClientPlugin("ApiHostAuthGuard", ::ApiHostGuardConfig) {
    val apiBaseUrl = pluginConfig.apiBaseUrl
    on(Send) { request ->
        if (!request.targetsApi(apiBaseUrl())) {
            request.headers.remove(HttpHeaders.Authorization)
        }
        proceed(request)
    }
}

internal fun HttpRequestBuilder.targetsApi(apiBaseUrl: String): Boolean {
    val api = runCatching { Url(apiBaseUrl) }.getOrNull() ?: return false
    val target = url.build()
    return target.protocol == api.protocol && target.host == api.host && target.port == api.port
}

internal fun HttpRequestBuilder.isAuthEndpoint(): Boolean =
    url.encodedPath.startsWith(ApiPaths.AUTH_PREFIX)
