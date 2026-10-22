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

    /** The signed-in user's payments, newest first, updated live as the server records them. */
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

    /** The key was already used for a different payment; the next attempt gets a new key. */
    IDEMPOTENCY_CONFLICT(retryable = false),
    UNEXPECTED_RESPONSE(retryable = false),
}
