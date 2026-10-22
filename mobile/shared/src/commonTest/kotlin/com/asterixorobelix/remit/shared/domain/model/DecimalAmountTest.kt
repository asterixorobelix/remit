package com.asterixorobelix.remit.shared.domain.model

import com.asterixorobelix.remit.shared.domain.model.DecimalParseResult.Failure
import com.asterixorobelix.remit.shared.domain.model.DecimalParseResult.Parsed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DecimalAmountTest {
    @Test
    fun parsesExactlyWithoutFloatingPoint() {
        val cases = mapOf(
            "10.50" to DecimalAmount(1050, 2),
            "10,50" to DecimalAmount(1050, 2),
            "0.1" to DecimalAmount(1, 1),
            ".5" to DecimalAmount(5, 1),
            "5." to DecimalAmount(5, 0),
            "007" to DecimalAmount(7, 0),
            "0" to DecimalAmount(0, 0),
            "0.00" to DecimalAmount(0, 2),
            " 12.3 " to DecimalAmount(123, 1),
            "9999999999999" to DecimalAmount(9_999_999_999_999, 0),
            "9999999999999.99999" to DecimalAmount(999_999_999_999_999_999, 5),
        )
        cases.forEach { (text, expected) -> assertEquals(Parsed(expected), DecimalAmount.parse(text), text) }
    }

    @Test
    fun rejectsAnythingThatIsNotAPlainDecimal() {
        val inputs = listOf("-1", "+1", "1e3", "1E-2", "NaN", "Infinity", "1,000.00", "1.000,00", "1 000")
        (inputs + listOf(".", ",", "abc", "$5", "\u0661"))
            .forEach { assertEquals(Failure.NOT_A_NUMBER, DecimalAmount.parse(it), it) }
    }

    @Test
    fun blankIsItsOwnFailure() {
        assertEquals(Failure.BLANK, DecimalAmount.parse(""))
        assertEquals(Failure.BLANK, DecimalAmount.parse(" \t "))
    }

    @Test
    fun capsWholeDigitsAsOutOfRange() {
        assertEquals(Failure.OUT_OF_RANGE, DecimalAmount.parse("10000000000000"))
        assertEquals(Failure.OUT_OF_RANGE, DecimalAmount.parse("99999999999999.99"))
        // Leading zeros are not significant.
        assertTrue(DecimalAmount.parse("0000000000000000000001") is Parsed)
    }

    @Test
    fun capsDecimalsAsTooPreciseNotAsTooLarge() {
        // What a JavaScript client sends for 0.1 + 0.2: wrong precision, not a huge amount.
        assertEquals(Failure.TOO_PRECISE, DecimalAmount.parse("0.30000000000000004"))
        assertEquals(Failure.TOO_PRECISE, DecimalAmount.parse("1.000000"))
        assertEquals(Parsed(DecimalAmount(100_000, 5)), DecimalAmount.parse("1.00000"))
    }

    @Test
    fun convertsToMinorUnitsOnlyWhenTheCurrencyCanRepresentIt() {
        assertEquals(Money(1050, Currency.USD), DecimalAmount(105, 1).toMoney(Currency.USD))
        assertEquals(Money(500, Currency.EUR), DecimalAmount(5, 0).toMoney(Currency.EUR))
        assertEquals(Money(1, Currency.USD), DecimalAmount(1, 2).toMoney(Currency.USD))
        assertNull(DecimalAmount(1, 3).toMoney(Currency.USD))
        // "1,000" is three decimals, never one dollar.
        assertNull(DecimalAmount(1000, 3).toMoney(Currency.USD))
    }

    @Test
    fun zeroIsDetectedAtAnyScale() {
        assertTrue(DecimalAmount(0, 2).isZero)
        assertTrue(!DecimalAmount(1, 5).isZero)
    }
}
