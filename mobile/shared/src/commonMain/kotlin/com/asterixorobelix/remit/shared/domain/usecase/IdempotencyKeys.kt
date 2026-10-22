package com.asterixorobelix.remit.shared.domain.usecase

import com.asterixorobelix.remit.shared.domain.model.PaymentRequest

/**
 * One idempotency key per payment the user is making. Submitting the same request again (a retry
 * after a timeout, or a second tap) reuses the key, so the server processes it at most once. A
 * changed request, or [reset] when the user starts a new payment, gets a fresh key.
 *
 * Not thread-safe on its own; [SendPaymentUseCase] serializes access.
 */
class IdempotencyKeys(private val newKey: () -> String) {
    private var current: Pair<PaymentRequest, String>? = null

    fun keyFor(request: PaymentRequest): String {
        val existing = current?.takeIf { (pending, _) -> pending == request }?.second
        return existing ?: newKey().also { current = request to it }
    }

    fun reset() {
        current = null
    }
}
