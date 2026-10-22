package com.asterixorobelix.remit.shared.data.history

import com.asterixorobelix.remit.shared.data.FirebaseSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Live payment records for one user, newest first. The Android source listens to Firestore with
 * a snapshot listener; each platform converts its SDK types to plain [PaymentRecord]s, so mapping
 * and validation stay in common code.
 */
interface PaymentHistorySource {
    fun observe(userId: String): Flow<List<PaymentRecord>>
}

/** A stored payment as plain values. Any field may be missing or malformed in stored data. */
data class PaymentRecord(
    val id: String,
    val recipientEmail: String?,
    val amountMinor: Long?,
    val currency: String?,
    val status: String?,
    val createdAtEpochMillis: Long?,
)

/** Firestore document fields, written by the backend (`payments/{paymentId}`). */
object PaymentFields {
    const val COLLECTION = "payments"
    const val SENDER_UID = "senderUid"
    const val RECIPIENT_EMAIL = "recipientEmail"
    const val AMOUNT_MINOR = "amountMinor"
    const val CURRENCY = "currency"
    const val STATUS = "status"
    const val CREATED_AT = "createdAt"

    /** History shows the most recent payments; paging is out of scope. */
    const val HISTORY_LIMIT = 100L
}

class PaymentHistoryUnavailableException(cause: Throwable? = null) :
    Exception("Payment history is unavailable", cause)

/** Firestore on Android; a source that always fails where Firebase is not wired. */
expect fun platformPaymentHistorySource(settings: FirebaseSettings): PaymentHistorySource

internal object UnsupportedPlatformHistorySource : PaymentHistorySource {
    override fun observe(userId: String): Flow<List<PaymentRecord>> = flow {
        throw PaymentHistoryUnavailableException(
            UnsupportedOperationException("Firebase is not set up in this build"),
        )
    }
}
