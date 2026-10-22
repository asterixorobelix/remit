package com.example.myproject.plugins

import io.ktor.http.HttpHeaders
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.event.Level
import java.util.UUID

private val REQUEST_ID_PATTERN = Regex("[A-Za-z0-9._-]{8,128}")
private val UNLOGGED_PATHS = setOf("/health", "/ready")

/**
 * Accepts a well-formed client `X-Request-ID` (otherwise generates one), echoes it in the response,
 * and exposes it to every log line as the `requestId` MDC value.
 *
 * Request logs contain method, path (no query string), status, and duration only — never headers,
 * bodies, tokens, or passwords.
 */
fun Application.configureMonitoring() {
    install(CallId) {
        retrieveFromHeader(HttpHeaders.XRequestId)
        generate { UUID.randomUUID().toString() }
        verify { REQUEST_ID_PATTERN.matches(it) }
        replyToHeader(HttpHeaders.XRequestId)
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("requestId")
        filter { call -> call.request.path() !in UNLOGGED_PATHS }
        format { call ->
            val status = call.response.status()?.value ?: "-"
            "$status ${call.request.httpMethod.value} ${call.request.path()} ${call.processingTimeMillis()}ms"
        }
    }
}
