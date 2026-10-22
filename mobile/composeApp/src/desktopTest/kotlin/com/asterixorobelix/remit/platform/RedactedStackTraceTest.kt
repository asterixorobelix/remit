package com.asterixorobelix.remit.platform

import kotlin.test.Test
import kotlin.test.assertContentEquals

/** JVM only: a redacted exception keeps the original stack trace, so crash reports still group. */
class RedactedStackTraceTest {
    @Test
    fun theRedactedCopyKeepsTheOriginalStackTrace() {
        val error = IllegalStateException("No account for bob@example.com")
        assertContentEquals(error.stackTrace, error.redacted().stackTrace)
    }
}
