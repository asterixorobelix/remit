package com.asterixorobelix.remit.shared.data.history

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentRecordMapperTest {
    private val record = PaymentRecord(
        id = "pay_1",
        recipientEmail = "alice@example.com",
        amountMinor = 1050,
        currency = "EUR",
        status = "COMPLETED",
        createdAtEpochMillis = 1_760_000_000_000,
    )

    @Test
    fun mapsACompleteRecord() {
        val payment = record.toPaymentOrNull()!!
        assertEquals(Money(1050, Currency.EUR), payment.amount)
        assertEquals(PaymentStatus.COMPLETED, payment.status)
        assertEquals(1_760_000_000_000, payment.createdAt.toEpochMilliseconds())
    }

    @Test
    fun skipsRecordsWithMissingOrBadFields() {
        listOf(
            record.copy(id = ""),
            record.copy(recipientEmail = null),
            record.copy(recipientEmail = "bad"),
            record.copy(amountMinor = null),
            record.copy(amountMinor = 0),
            record.copy(amountMinor = -100),
            record.copy(currency = null),
            record.copy(currency = "XXX"),
            record.copy(createdAtEpochMillis = null),
        ).forEach { assertNull(it.toPaymentOrNull(), it.toString()) }
    }

    @Test
    fun missingStatusIsUnknownNotAnError() {
        assertEquals(PaymentStatus.UNKNOWN, record.copy(status = null).toPaymentOrNull()?.status)
    }
}
