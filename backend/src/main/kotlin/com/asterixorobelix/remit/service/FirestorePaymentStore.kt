package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentStatus
import com.asterixorobelix.remit.domain.StoredPayment
import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Payments in Firestore. The idempotency record and the payment are written in one transaction:
 * two concurrent requests with the same key both read "no record", Firestore makes one of them
 * retry, and the retry sees the record and returns the original payment. So a key can never
 * produce two payments, even under load.
 *
 * Documents: `payments/{id}` (read by the app, see firebase/firestore.rules) and
 * `idempotencyKeys/{senderUid}_{key}` (server-only).
 */
class FirestorePaymentStore(private val firestore: Firestore) : PaymentStore {
    override suspend fun createOnce(
        idempotencyKey: String,
        fingerprint: String,
        payment: StoredPayment,
    ): CreateOutcome = withContext(Dispatchers.IO) {
        val keyRef = firestore.collection(IDEMPOTENCY_KEYS).document("${payment.senderUid}_$idempotencyKey")
        val paymentRef = firestore.collection(PAYMENTS).document(payment.id)
        firestore.runTransaction { transaction ->
            val record = transaction.get(keyRef).get()
            val originalId = record.getString(PAYMENT_ID)
            when {
                !record.exists() -> {
                    transaction.create(paymentRef, payment.toFields())
                    transaction.create(keyRef, keyFields(fingerprint, payment))
                    CreateOutcome.Created(payment)
                }

                record.getString(FINGERPRINT) != fingerprint || originalId == null -> CreateOutcome.KeyReused

                else -> CreateOutcome.Replayed(
                    transaction.get(firestore.collection(PAYMENTS).document(originalId)).get().toStoredPayment(),
                )
            }
        }.get()
    }

    private fun keyFields(fingerprint: String, payment: StoredPayment) = mapOf(
        PAYMENT_ID to payment.id,
        FINGERPRINT to fingerprint,
        "senderUid" to payment.senderUid,
        "createdAt" to payment.createdAt.toTimestamp(),
    )

    companion object {
        const val PAYMENTS = "payments"
        const val IDEMPOTENCY_KEYS = "idempotencyKeys"
        private const val PAYMENT_ID = "paymentId"
        private const val FINGERPRINT = "fingerprint"
    }
}

/** Field names the app reads (`PaymentFields` in the mobile shared module). */
internal fun StoredPayment.toFields(): Map<String, Any> = mapOf(
    "senderUid" to senderUid,
    "recipientEmail" to recipientEmail,
    "amountMinor" to amountMinor,
    "currency" to currency.code,
    "status" to status.name,
    "createdAt" to createdAt.toTimestamp(),
)

internal fun DocumentSnapshot.toStoredPayment(): StoredPayment = StoredPayment(
    id = id,
    senderUid = checkNotNull(getString("senderUid")),
    recipientEmail = checkNotNull(getString("recipientEmail")),
    amountMinor = checkNotNull(getLong("amountMinor")),
    currency = checkNotNull(getString("currency")?.let(Currency::fromCode)),
    status = PaymentStatus.valueOf(checkNotNull(getString("status"))),
    createdAt = checkNotNull(getTimestamp("createdAt")).toDate().toInstant(),
)

private fun Instant.toTimestamp(): Timestamp = Timestamp.ofTimeSecondsAndNanos(epochSecond, nano)
