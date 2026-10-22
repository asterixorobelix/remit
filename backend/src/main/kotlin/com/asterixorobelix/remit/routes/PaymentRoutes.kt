package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.dto.CreatePaymentRequest
import com.asterixorobelix.remit.dto.ErrorResponse
import com.asterixorobelix.remit.dto.toResponse
import com.asterixorobelix.remit.service.PaymentResult
import com.asterixorobelix.remit.service.PaymentService
import com.asterixorobelix.remit.service.TokenVerificationUnavailableException
import com.asterixorobelix.remit.service.TokenVerifier
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.request.receive
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.post

const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"
const val IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed"
private val IDEMPOTENCY_KEY = Regex("^[A-Za-z0-9._:-]{8,255}$")

/** The payments feature: Firebase-backed, or absent when the server has no Firebase project. */
class PaymentsFeature(val service: PaymentService, val tokens: TokenVerifier)

/**
 * `POST /api/v1/payments`. Order of checks: payments configured (503), token verifiable (503),
 * caller signed in (401), idempotency key present and well formed (400), body valid (400), key not
 * reused for a different payment (409). A replay answers 201 with the original payment and
 * `Idempotent-Replayed: true`, or 500 when that payment can no longer be read (it is not sent again).
 */
fun Route.paymentRoutes(feature: PaymentsFeature?) {
    post("/api/v1/payments") {
        val userId = try {
            feature?.let { call.authenticatedUser(it.tokens) }
        } catch (unavailable: TokenVerificationUnavailableException) {
            call.application.log.warn("ID token verification is unavailable", unavailable)
            return@post call.fail(HttpStatusCode.ServiceUnavailable, AUTH_UNAVAILABLE)
        }
        val key = call.request.headers[IDEMPOTENCY_KEY_HEADER]
        when {
            feature == null -> call.fail(HttpStatusCode.ServiceUnavailable, PAYMENTS_UNAVAILABLE)
            userId == null -> call.fail(HttpStatusCode.Unauthorized, UNAUTHENTICATED)
            key.isNullOrBlank() -> call.fail(HttpStatusCode.BadRequest, KEY_MISSING)
            !IDEMPOTENCY_KEY.matches(key) -> call.fail(HttpStatusCode.BadRequest, KEY_INVALID)
            else -> call.respondTo(feature.service.create(userId, key, call.receive<CreatePaymentRequest>()))
        }
    }
}

private suspend fun RoutingCall.authenticatedUser(tokens: TokenVerifier): String? {
    val token = request.headers[HttpHeaders.Authorization]
        ?.takeIf { it.startsWith(BEARER, ignoreCase = true) }
        ?.substring(BEARER.length)
        ?.trim()
    return token?.takeIf(String::isNotEmpty)?.let { tokens.userId(it) }
}

private suspend fun RoutingCall.respondTo(result: PaymentResult) {
    when (result) {
        is PaymentResult.Created -> respond(HttpStatusCode.Created, result.payment.toResponse())

        is PaymentResult.Replayed -> {
            response.header(IDEMPOTENT_REPLAYED_HEADER, "true")
            respond(HttpStatusCode.Created, result.payment.toResponse())
        }

        is PaymentResult.Invalid -> respond(
            HttpStatusCode.BadRequest,
            ErrorResponse("Invalid payment", "VALIDATION_FAILED", result.codes.map { it.name }.sorted()),
        )

        PaymentResult.KeyReused -> fail(HttpStatusCode.Conflict, KEY_REUSED)

        is PaymentResult.ReplayUnavailable -> {
            application.log.error("Payment {} for a replayed $IDEMPOTENCY_KEY_HEADER is unreadable", result.paymentId)
            fail(HttpStatusCode.InternalServerError, REPLAY_UNAVAILABLE)
        }
    }
}

private suspend fun RoutingCall.fail(status: HttpStatusCode, error: ErrorResponse) = respond(status, error)

private const val BEARER = "Bearer "
private val PAYMENTS_UNAVAILABLE = ErrorResponse("Payments are not configured", "PAYMENTS_UNAVAILABLE")
private val AUTH_UNAVAILABLE = ErrorResponse("Sign-in could not be verified; try again", "AUTH_UNAVAILABLE")
private val UNAUTHENTICATED = ErrorResponse("A valid Firebase ID token is required", "UNAUTHENTICATED")
private val KEY_MISSING = ErrorResponse("$IDEMPOTENCY_KEY_HEADER header is required", "IDEMPOTENCY_KEY_MISSING")
private val KEY_INVALID = ErrorResponse(
    "$IDEMPOTENCY_KEY_HEADER must be 8-255 characters of letters, digits, '.', '_', ':' or '-'",
    "IDEMPOTENCY_KEY_INVALID",
)
private val KEY_REUSED = ErrorResponse(
    "$IDEMPOTENCY_KEY_HEADER was already used for a different payment",
    "IDEMPOTENCY_KEY_REUSED",
)
private val REPLAY_UNAVAILABLE = ErrorResponse(
    "The payment for this $IDEMPOTENCY_KEY_HEADER could not be read; it was not sent again",
    "REPLAY_UNAVAILABLE",
)
