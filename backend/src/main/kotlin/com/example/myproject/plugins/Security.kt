package com.example.myproject.plugins

import com.example.myproject.configuration.JwtConfig
import com.example.myproject.configuration.RateLimitConfig
import com.example.myproject.service.JwtTokens
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.Route
import kotlin.time.Duration.Companion.minutes

const val JWT_AUTH = "auth-jwt"

/** Login and registration: strict per-client bucket. */
val AUTH_RATE_LIMIT = RateLimitName("auth")

/** Token refresh and logout: lenient per-client bucket. */
val REFRESH_RATE_LIMIT = RateLimitName("refresh")

fun Application.configureSecurity(jwtConfig: JwtConfig, rateLimit: RateLimitConfig) {
    if (rateLimit.enabled) {
        install(RateLimit) {
            register(AUTH_RATE_LIMIT) {
                rateLimiter(limit = rateLimit.authRequestsPerMinute, refillPeriod = 1.minutes)
                requestKey { call -> call.request.origin.remoteHost }
            }
            register(REFRESH_RATE_LIMIT) {
                rateLimiter(limit = rateLimit.refreshRequestsPerMinute, refillPeriod = 1.minutes)
                requestKey { call -> call.request.origin.remoteHost }
            }
        }
    }

    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = jwtConfig.realm
            verifier(JwtTokens(jwtConfig).accessVerifier)
            validate { credential ->
                credential.payload.subject?.toLongOrNull()?.let { JWTPrincipal(credential.payload) }
            }
        }
    }
}

/** Applies [name] when rate limiting is enabled (the RateLimit plugin is installed). */
fun Route.rateLimited(name: RateLimitName, enabled: Boolean, build: Route.() -> Unit) {
    if (enabled) rateLimit(name, build) else build()
}

/** The user id of an authenticated call; `validate` guarantees a numeric subject. */
val ApplicationCall.userId: Long
    get() = checkNotNull(principal<JWTPrincipal>()?.subject?.toLongOrNull()) { "No authenticated user" }
