package com.asterixorobelix.remit.data.payments

import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.platform.AppDispatchers
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.PendingPayment
import com.asterixorobelix.remit.shared.domain.repository.PendingPaymentStore
import kotlinx.coroutines.withContext

/** The payment in flight, on disk, so it survives the app being killed mid-send. */
class SqlDelightPendingPaymentStore(
    private val database: AppDatabase,
    private val dispatchers: AppDispatchers,
) : PendingPaymentStore {
    private val queries get() = database.pendingPaymentQueries

    /** A row that no longer parses (for example a currency removed in an update) is dropped. */
    override suspend fun load(): PendingPayment? = withContext(dispatchers.io) {
        val row = queries.get().executeAsOneOrNull() ?: return@withContext null
        val recipient = EmailAddress.parse(row.recipient_email)
        val currency = Currency.fromCode(row.currency)
        if (recipient == null || currency == null || row.amount_minor <= 0) {
            queries.clear()
            null
        } else {
            PendingPayment(PaymentRequest(recipient, Money(row.amount_minor, currency)), row.idempotency_key)
        }
    }

    override suspend fun save(pending: PendingPayment) {
        val amount = pending.request.amount
        withContext(dispatchers.io) {
            queries.save(
                pending.idempotencyKey,
                pending.request.recipient.value,
                amount.minorUnits,
                amount.currency.code,
            )
        }
    }

    override suspend fun clear() {
        withContext(dispatchers.io) { queries.clear() }
    }
}
