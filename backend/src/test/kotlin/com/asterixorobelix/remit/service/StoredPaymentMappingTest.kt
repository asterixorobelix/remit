package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentStatus
import com.asterixorobelix.remit.domain.StoredPayment
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StoredPaymentMappingTest {
    private val payment = StoredPayment(
        id = "pay_1",
        senderUid = "alice",
        recipientEmail = "bob@example.com",
        amountMinor = 1050,
        currency = Currency.USD,
        status = PaymentStatus.COMPLETED,
        createdAt = Instant.parse("2026-10-22T08:30:00.123Z"),
    )
    private val fields = payment.toFields()

    @Test
    fun readsBackWhatTheStoreWrites() {
        assertEquals(payment, storedPaymentOrNull("pay_1", fields))
    }

    @Test
    fun aMissingFieldIsNullNotAnException() {
        fields.keys.forEach { name -> assertNull(storedPaymentOrNull("pay_1", fields - name), name) }
    }

    @Test
    fun aMalformedFieldIsNullNotAnException() {
        listOf(
            "senderUid" to 42L,
            "amountMinor" to "1050",
            "amountMinor" to 10.5,
            "amountMinor" to 0L,
            "currency" to "GBP",
            "status" to "REFUNDED",
            "status" to "completed",
            "createdAt" to "2026-10-22T08:30:00Z",
        ).forEach { (name, value) ->
            assertNull(storedPaymentOrNull("pay_1", fields + (name to value)), "$name=$value")
        }
    }
}
