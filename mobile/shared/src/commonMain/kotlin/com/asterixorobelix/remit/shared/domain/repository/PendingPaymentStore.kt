package com.asterixorobelix.remit.shared.domain.repository

import com.asterixorobelix.remit.shared.domain.model.PaymentRequest

/** A payment that has been, or is about to be, sent, with the idempotency key it is sent under. */
data class PendingPayment(val request: PaymentRequest, val idempotencyKey: String)

/**
 * Durable storage for the one payment in flight. The key is saved before the request goes out,
 * so if the app is killed mid-send, the payment can be retried under the same key after a restart
 * and the server still processes it at most once. The host app provides it (SQLDelight on Android).
 */
interface PendingPaymentStore {
    suspend fun load(): PendingPayment?

    suspend fun save(pending: PendingPayment)

    suspend fun clear()
}

/** For tests and hosts without a database: survives retries, not process death. */
class InMemoryPendingPaymentStore : PendingPaymentStore {
    private var pending: PendingPayment? = null

    override suspend fun load(): PendingPayment? = pending

    override suspend fun save(pending: PendingPayment) {
        this.pending = pending
    }

    override suspend fun clear() {
        pending = null
    }
}
