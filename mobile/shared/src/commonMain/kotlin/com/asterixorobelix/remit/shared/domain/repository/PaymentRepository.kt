package com.asterixorobelix.remit.shared.domain.repository

import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import kotlinx.coroutines.flow.Flow

interface PaymentRepository {
    /**
     * Sends [request] to the payments API. The server processes each [idempotencyKey] once:
     * repeating a key returns the original payment instead of charging again.
     */
    suspend fun send(request: PaymentRequest, idempotencyKey: String): SendPaymentResult

    /**
     * The signed-in user's payments, newest first, updated live as the server records them.
     * Fails with [PaymentHistoryException].
     */
    fun observeHistory(): Flow<List<Payment>>
}

sealed interface SendPaymentResult {
    /**
     * [replayed] means the server had already processed this payment (same idempotency key), for
     * example before the app was killed, and returned the original instead of a new payment.
     */
    data class Success(val payment: Payment, val replayed: Boolean = false) : SendPaymentResult

    /** Rejected by validation, on the device or by the server. */
    data class Invalid(val errors: Set<ValidationError>) : SendPaymentResult

    data class Failed(val reason: PaymentFailure) : SendPaymentResult
}

/**
 * Why a payment did not go through. Every retry reuses the idempotency key, so retrying is always
 * safe; [retryable] says whether trying again can help.
 */
enum class PaymentFailure(val retryable: Boolean) {
    NETWORK(retryable = true),
    TIMEOUT(retryable = true),
    SERVER(retryable = true),
    RATE_LIMITED(retryable = true),
    UNAUTHORIZED(retryable = false),

    /** This build cannot reach the payments service at all (Firebase is not set up in it). */
    NOT_SET_UP(retryable = false),

    /** The key was already used for a different payment; the next attempt gets a new key. */
    IDEMPOTENCY_CONFLICT(retryable = false),
    UNEXPECTED_RESPONSE(retryable = false),
}

/** History stopped; [reason] says why, so the screen blames the connection only when it is at fault. */
class PaymentHistoryException(val reason: HistoryFailure, cause: Throwable? = null) :
    Exception("Payment history failed: $reason", cause)

/** Why history could not be loaded; [retryable] says whether trying again can help. */
enum class HistoryFailure(val retryable: Boolean) {
    /** Offline, or Firebase could not be reached. */
    NETWORK(retryable = true),

    /** Firebase refused the sign-in or the query. */
    UNAVAILABLE(retryable = true),

    /** This build cannot reach the payments service at all (Firebase is not set up in it). */
    NOT_SET_UP(retryable = false),
}
