package com.asterixorobelix.remit.shared.data.history

import com.asterixorobelix.remit.shared.data.FirebaseSettings
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

actual fun platformPaymentHistorySource(settings: FirebaseSettings): PaymentHistorySource {
    val firestore = FirebaseFirestore.getInstance()
    settings.emulatorHost?.let { firestore.useEmulator(it, FirebaseSettings.FIRESTORE_EMULATOR_PORT) }
    return FirestorePaymentHistorySource(firestore)
}

/**
 * A Firestore snapshot listener as a Flow: each change to the user's payments emits the full list,
 * and cancelling the collector removes the listener.
 */
internal class FirestorePaymentHistorySource(private val firestore: FirebaseFirestore) : PaymentHistorySource {
    override fun observe(userId: String): Flow<List<PaymentRecord>> = callbackFlow {
        val registration = firestore.collection(PaymentFields.COLLECTION)
            .whereEqualTo(PaymentFields.SENDER_UID, userId)
            .orderBy(PaymentFields.CREATED_AT, Query.Direction.DESCENDING)
            .limit(PaymentFields.HISTORY_LIMIT)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(PaymentHistoryUnavailableException(error))
                } else if (snapshot != null) {
                    trySend(snapshot.documents.map { it.toRecord() })
                }
            }
        awaitClose { registration.remove() }
    }
}

/** Reads fields defensively (`as?`): a wrongly typed field becomes null, never a crash. */
private fun DocumentSnapshot.toRecord() = PaymentRecord(
    id = id,
    recipientEmail = get(PaymentFields.RECIPIENT_EMAIL) as? String,
    amountMinor = (get(PaymentFields.AMOUNT_MINOR) as? Long),
    currency = get(PaymentFields.CURRENCY) as? String,
    status = get(PaymentFields.STATUS) as? String,
    createdAtEpochMillis = (get(PaymentFields.CREATED_AT) as? Timestamp)?.toDate()?.time,
)
