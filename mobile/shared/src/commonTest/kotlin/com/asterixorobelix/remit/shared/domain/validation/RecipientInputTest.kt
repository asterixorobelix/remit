package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import kotlin.test.Test
import kotlin.test.assertEquals

class RecipientInputTest {
    private fun type(previous: String, proposed: String) = FilterRecipientInputUseCase()(previous, proposed)

    /** A 64-character local part and a 189-character domain: the longest address validation accepts. */
    private val longest = "a".repeat(64) + "@" + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61)

    @Test
    fun acceptsEditsUpToTheLongestValidAddress() {
        assertEquals(RecipientInput.MAX_LENGTH, longest.length)
        assertEquals(longest, EmailAddress.parse(longest)?.value)
        assertEquals(longest, type("", longest))
        assertEquals("bob@example.com", type("bob@example.co", "bob@example.com"))
    }

    @Test
    fun ignoresAnEditThatWouldGoPastTheLimit() {
        assertEquals(longest, type(longest, longest + "d"))
        assertEquals("bob@", type("bob@", "bob@" + "x".repeat(RecipientInput.MAX_LENGTH)))
    }

    @Test
    fun anOverLongValueCanStillBeShortened() {
        val tooLong = "x".repeat(RecipientInput.MAX_LENGTH + 5)
        assertEquals(tooLong.dropLast(1), type(tooLong, tooLong.dropLast(1)))
        assertEquals("", type(tooLong, ""))
    }
}
