package com.asterixorobelix.remit.presentation.payments

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class PaymentFormattingTest {
    @Test
    fun moneyShowsSymbolGroupingAndExactCents() {
        assertEquals("$10.50", Money(1050, Currency.USD).formatted())
        assertEquals("€0.05", Money(5, Currency.EUR).formatted())
        assertEquals("$10,000.00", Money(1_000_000, Currency.USD).formatted())
        assertEquals("€1,234,567.89", Money(123_456_789, Currency.EUR).formatted())
    }

    @Test
    fun groupsThousands() {
        assertEquals("1", groupThousands("1"))
        assertEquals("999", groupThousands("999"))
        assertEquals("1,000", groupThousands("1000"))
        assertEquals("10,000", groupThousands("10000"))
    }

    @Test
    fun timestampsUseTheGivenZone() {
        val instant = Instant.parse("2026-10-22T08:30:00Z")
        assertEquals("22 Oct 2026, 08:30", instant.formattedLocal(TimeZone.UTC))
        assertEquals("22 Oct 2026, 10:30", instant.formattedLocal(TimeZone.of("Africa/Johannesburg")))
        assertEquals(
            "1 Jan 2027, 00:05",
            Instant.parse("2026-12-31T22:05:00Z").formattedLocal(TimeZone.of("Africa/Johannesburg")),
        )
    }

    @Test
    fun everyCodeHasAMessage() {
        // Exhaustive `when`s guarantee this at compile time; this guards against shared resources.
        assertEquals(ValidationError.entries.size, ValidationError.entries.map { it.message }.toSet().size)
        assertEquals(PaymentFailure.entries.size, PaymentFailure.entries.map { it.message }.toSet().size)
        assertEquals(PaymentStatus.entries.size, PaymentStatus.entries.map { it.label }.toSet().size)
    }
}
