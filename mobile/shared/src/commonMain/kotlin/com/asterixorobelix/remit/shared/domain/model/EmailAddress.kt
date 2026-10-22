package com.asterixorobelix.remit.shared.domain.model

import kotlin.jvm.JvmInline

/**
 * A recipient address, trimmed and lower-cased (providers treat addresses case-insensitively, and
 * one spelling keeps history and idempotency comparisons simple). The check is deliberately
 * pragmatic: dot-separated atoms, an `@`, and a domain with a letters-only top-level label. Proving
 * an address exists needs a confirmation email, which no pattern can replace.
 */
@JvmInline
value class EmailAddress private constructor(val value: String) {
    override fun toString(): String = value

    companion object {
        /** RFC 5321 path limit, minus the angle brackets. */
        const val MAX_LENGTH = 254
        private const val MAX_LOCAL_LENGTH = 64
        private const val ATOM = "[A-Za-z0-9!#\$%&'*+/=?^_`{|}~-]+"
        private const val LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
        private val PATTERN = Regex("^$ATOM(?:\\.$ATOM)*@(?:$LABEL\\.)+[A-Za-z]{2,63}$")

        /** Normalizes [input] and returns it, or null when it is not a plausible address. */
        fun parse(input: String): EmailAddress? {
            val normalized = input.trim().lowercase()
            val localPart = normalized.substringBefore('@')
            val plausible = normalized.length <= MAX_LENGTH &&
                localPart.length <= MAX_LOCAL_LENGTH &&
                PATTERN.matches(normalized)
            return if (plausible) EmailAddress(normalized) else null
        }
    }
}
