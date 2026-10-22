package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.StoredPayment
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Where payments and their idempotency records live: Firestore in production. */
interface PaymentStore {
    /**
     * Atomically records [payment] under its sender and [idempotencyKey], unless the sender used
     * that key before. A repeated key with the same [fingerprint] (same recipient, amount, and
     * currency) returns the original payment; a different fingerprint is a conflict.
     */
    suspend fun createOnce(idempotencyKey: String, fingerprint: String, payment: StoredPayment): CreateOutcome
}

sealed interface CreateOutcome {
    data class Created(val payment: StoredPayment) : CreateOutcome

    data class Replayed(val payment: StoredPayment) : CreateOutcome

    data object KeyReused : CreateOutcome
}

/** For tests: the same contract as Firestore, serialized with a mutex. */
class InMemoryPaymentStore : PaymentStore {
    private val mutex = Mutex()
    private val records = mutableMapOf<Pair<String, String>, Pair<String, StoredPayment>>()

    val payments: List<StoredPayment> get() = records.values.map { it.second }

    override suspend fun createOnce(idempotencyKey: String, fingerprint: String, payment: StoredPayment) =
        mutex.withLock {
            val existing = records[payment.senderUid to idempotencyKey]
            when {
                existing == null -> {
                    records[payment.senderUid to idempotencyKey] = fingerprint to payment
                    CreateOutcome.Created(payment)
                }

                existing.first == fingerprint -> CreateOutcome.Replayed(existing.second)

                else -> CreateOutcome.KeyReused
            }
        }
}
