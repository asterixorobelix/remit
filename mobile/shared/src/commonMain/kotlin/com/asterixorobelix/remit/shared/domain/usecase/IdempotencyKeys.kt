package com.asterixorobelix.remit.shared.domain.usecase

import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.PendingPayment
import com.asterixorobelix.remit.shared.domain.repository.PendingPaymentStore

/**
 * One idempotency key per payment the user is making. Submitting the same request again (a retry
 * after a timeout, a second tap, or a retry after the app restarted) reuses the key, so the server
 * processes it at most once. A changed request, or [reset] when the user starts a new payment,
 * gets a fresh key. The key is persisted in [store] before it is returned, that is, before the
 * request is sent (write-ahead).
 *
 * Not thread-safe on its own; [SendPaymentUseCase] serializes access.
 */
class IdempotencyKeys(
    private val store: PendingPaymentStore,
    private val newKey: () -> String,
) {
    suspend fun keyFor(request: PaymentRequest): String {
        val existing = store.load()?.takeIf { it.request == request }?.idempotencyKey
        return existing ?: newKey().also { store.save(PendingPayment(request, it)) }
    }

    suspend fun pending(): PendingPayment? = store.load()

    suspend fun reset() = store.clear()
}
