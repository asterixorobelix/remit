package com.asterixorobelix.remit.data.network

import com.asterixorobelix.remit.util.generateId
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders

/** Adds a unique `X-Request-ID` to every request (kept identical across retries of that request). */
internal val RequestIdPlugin = createClientPlugin("RequestId") {
    onRequest { request, _ ->
        if (!request.headers.contains(HttpHeaders.XRequestId)) {
            request.headers.append(HttpHeaders.XRequestId, generateId())
        }
    }
}
