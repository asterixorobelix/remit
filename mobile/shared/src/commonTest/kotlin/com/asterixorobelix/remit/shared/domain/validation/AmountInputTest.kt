package com.asterixorobelix.remit.shared.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class AmountInputTest {
    private fun type(previous: String, proposed: String) = FilterAmountInputUseCase()(previous, proposed)

    @Test
    fun keepsDigitsAndOneSeparator() {
        assertEquals("10.50", type("10.5", "10.50"))
        assertEquals("10,5", type("10", "10,5"))
        assertEquals(".5", type("", ".5"))
        assertEquals("", type("5", ""))
    }

    @Test
    fun dropsLettersSignsSpacesAndSymbols() {
        assertEquals("", type("", "tttttt"))
        assertEquals("10", type("1", "1t0"))
        assertEquals("5", type("", "-5"))
        assertEquals("1000", type("", " 1 000 "))
        assertEquals("10", type("", "€10"))
        assertEquals("13", type("", "1e3"))
    }

    @Test
    fun dropsNonAsciiDigits() {
        assertEquals("", type("", "١٠"))
    }

    @Test
    fun ignoresAnEditThatWouldAddASecondSeparator() {
        assertEquals("1,000", type("1,000", "1,000."))
        assertEquals("", type("", "$1,234.56"))
        assertEquals("7", type("7", "7.."))
    }

    @Test
    fun letsExtraDecimalsThroughForValidationToFlag() {
        // "1,000" in USD must reach validation (too many decimals), not become "1,00".
        assertEquals("1,000", type("1,00", "1,000"))
    }

    @Test
    fun capsTheLength() {
        val longest = "9".repeat(AmountInput.MAX_LENGTH)
        assertEquals(longest, type("", longest))
        assertEquals(longest, type(longest, longest + "9"))
    }
}
