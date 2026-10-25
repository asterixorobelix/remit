package com.asterixorobelix.remit.shared.data.history

import com.asterixorobelix.remit.shared.data.BackendNotSetUpException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Live payment records for one user, newest first. On Android a Firestore snapshot listener
 * implements it (`firebaseBackendModule`); an implementation converts its SDK types to plain
 * [PaymentRecord]s, so mapping and validation stay in common code.
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

/** [isNetworkError]: the history store could not be reached, so retrying later can help. */
class PaymentHistoryUnavailableException(val isNetworkError: Boolean = false, cause: Throwable? = null) :
    Exception("Payment history is unavailable", cause)

internal object UnsupportedPlatformHistorySource : PaymentHistorySource {
    override fun observe(userId: String): Flow<List<PaymentRecord>> = flow {
        throw PaymentHistoryUnavailableException(cause = BackendNotSetUpException())
    }
}
