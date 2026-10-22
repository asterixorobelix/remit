package com.asterixorobelix.remit.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EmailAddressTest {
    @Test
    fun acceptsCommonAddressesAndNormalizesThem() {
        assertEquals("alice@example.com", EmailAddress.parse("  Alice@Example.COM ")?.value)
        listOf("a.b+tag@sub.example.co.uk", "o'brien@example.ie", "x_y-z@example-domain.io", "1@example.museum")
            .forEach { assertEquals(it, EmailAddress.parse(it)?.value, it) }
    }

    @Test
    fun rejectsImplausibleAddresses() {
        listOf(
            "", "plain", "a@", "@example.com", "a@example", "a@example.c", "a@example.123", "a..b@example.com",
            ".a@example.com", "a.@example.com", "a@-example.com", "a@example-.com", "a b@example.com",
            "a@b@example.com", "a@exa_mple.com", "a".repeat(65) + "@example.com",
        ).forEach { assertNull(EmailAddress.parse(it), it) }
    }

    @Test
    fun enforcesTheOverallLengthLimit() {
        val domain = List(4) { "b".repeat(61) }.joinToString(".") + ".io"
        val atLimit = "a".repeat(EmailAddress.MAX_LENGTH - domain.length - 1) + "@" + domain
        assertEquals(EmailAddress.MAX_LENGTH, atLimit.length)
        assertEquals(atLimit, EmailAddress.parse(atLimit)?.value)
        assertNull(EmailAddress.parse("a$atLimit"))
    }

    @Test
    fun printsAsTheAddress() {
        assertEquals("alice@example.com", EmailAddress.parse("alice@example.com").toString())
    }
}
