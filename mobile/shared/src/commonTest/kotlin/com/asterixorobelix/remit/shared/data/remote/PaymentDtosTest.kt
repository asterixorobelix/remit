package com.asterixorobelix.remit.shared.data.remote

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import com.asterixorobelix.remit.shared.testing.paymentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentDtosTest {
    private val valid = PaymentResponseDto(
        id = "pay_1",
        recipientEmail = "Alice@Example.com",
        amount = "10.5",
        currency = "EUR",
        status = "COMPLETED",
        createdAt = "2026-10-22T08:30:00Z",
    )

    @Test
    fun requestUsesCanonicalAmountText() {
        val dto = paymentRequest(amount = Money(5, Currency.USD)).toDto()
        assertEquals(CreatePaymentRequestDto("alice@example.com", "0.05", "USD"), dto)
    }

    @Test
    fun mapsAValidResponse() {
        val payment = valid.toDomainOrNull()!!
        assertEquals("pay_1", payment.id)
        assertEquals("alice@example.com", payment.recipient.value)
        assertEquals(Money(1050, Currency.EUR), payment.amount)
        assertEquals(PaymentStatus.COMPLETED, payment.status)
        assertEquals(1_792_657_800_000, payment.createdAt.toEpochMilliseconds())
    }

    @Test
    fun anyMalformedFieldRejectsTheWholeResponse() {
        listOf(
            valid.copy(id = " "),
            valid.copy(recipientEmail = "not-an-email"),
            valid.copy(amount = "10.505"),
            valid.copy(amount = "-1"),
            valid.copy(currency = "GBP"),
            valid.copy(createdAt = "yesterday"),
        ).forEach { assertNull(it.toDomainOrNull(), it.toString()) }
    }

    @Test
    fun unknownStatusIsKept() {
        assertEquals(PaymentStatus.UNKNOWN, valid.copy(status = "REVERSED").toDomainOrNull()?.status)
    }
}
