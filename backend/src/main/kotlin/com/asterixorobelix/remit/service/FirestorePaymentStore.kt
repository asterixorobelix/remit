package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentStatus
import com.asterixorobelix.remit.domain.StoredPayment
import com.google.cloud.Timestamp
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.Transaction
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
 * `idempotencyKeys/{senderUid}_{key}` (server-only). A replay whose original payment was deleted or
 * cannot be read is [CreateOutcome.ReplayUnavailable], never a second payment.
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

                record.getString(FINGERPRINT) != fingerprint -> CreateOutcome.KeyReused

                else -> replay(transaction, originalId)
            }
        }.get()
    }

    private fun replay(transaction: Transaction, paymentId: String?): CreateOutcome {
        val original = paymentId?.let { transaction.get(firestore.collection(PAYMENTS).document(it)).get() }
        return original?.toStoredPaymentOrNull()?.let(CreateOutcome::Replayed)
            ?: CreateOutcome.ReplayUnavailable(paymentId)
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

/** Null for a missing document; see [storedPaymentOrNull]. */
internal fun DocumentSnapshot.toStoredPaymentOrNull(): StoredPayment? = data?.let { storedPaymentOrNull(id, it) }

/**
 * Null for a missing or malformed field, or a status this build does not know (as the app's
 * `toPaymentOrNull`), so an unreadable document is reported instead of throwing.
 */
internal fun storedPaymentOrNull(id: String, fields: Map<String, Any?>): StoredPayment? {
    val senderUid = fields["senderUid"] as? String
    val recipientEmail = fields["recipientEmail"] as? String
    val amountMinor = (fields["amountMinor"] as? Long)?.takeIf { it > 0 }
    val currency = (fields["currency"] as? String)?.let(Currency::fromCode)
    val status = (fields["status"] as? String)?.let { name -> PaymentStatus.entries.firstOrNull { it.name == name } }
    val createdAt = (fields["createdAt"] as? Timestamp)?.toDate()?.toInstant()
    return when {
        senderUid == null || recipientEmail == null || amountMinor == null -> null
        currency == null || status == null || createdAt == null -> null
        else -> StoredPayment(id, senderUid, recipientEmail, amountMinor, currency, status, createdAt)
    }
}

private fun Instant.toTimestamp(): Timestamp = Timestamp.ofTimeSecondsAndNanos(epochSecond, nano)
