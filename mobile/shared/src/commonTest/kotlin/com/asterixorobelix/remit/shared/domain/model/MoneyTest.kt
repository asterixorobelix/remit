package com.asterixorobelix.remit.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyTest {
    @Test
    fun formatsWithExactlyTheCurrencyDecimals() {
        assertEquals("10.50", Money(1050, Currency.USD).toDecimalString())
        assertEquals("0.01", Money(1, Currency.EUR).toDecimalString())
        assertEquals("0.00", Money(0, Currency.USD).toDecimalString())
        assertEquals("10000.00", Money(1_000_000, Currency.USD).toDecimalString())
        assertEquals("-0.05", Money(-5, Currency.USD).toDecimalString())
    }

    @Test
    fun formatsTheExtremesWithoutOverflow() {
        assertEquals("92233720368547758.07", Money(Long.MAX_VALUE, Currency.USD).toDecimalString())
        assertEquals("-92233720368547758.08", Money(Long.MIN_VALUE, Currency.USD).toDecimalString())
    }

    @Test
    fun addingTenAndTwentyCentsIsExact() {
        val parse = { text: String ->
            ((DecimalAmount.parse(text) as DecimalParseResult.Parsed).amount.toMoney(Currency.USD))!!
        }
        val sum = Money(parse("0.10").minorUnits + parse("0.20").minorUnits, Currency.USD)
        assertEquals("0.30", sum.toDecimalString())
    }

    @Test
    fun comparesWithinOneCurrencyOnly() {
        assertTrue(Money(2, Currency.USD) > Money(1, Currency.USD))
        assertFailsWith<IllegalArgumentException> { Money(1, Currency.USD) < Money(1, Currency.EUR) }
    }

    @Test
    fun currencyCodesAreExact() {
        assertEquals(Currency.EUR, Currency.fromCode("EUR"))
        assertEquals(null, Currency.fromCode("eur"))
        assertEquals(null, Currency.fromCode("GBP"))
    }
}
