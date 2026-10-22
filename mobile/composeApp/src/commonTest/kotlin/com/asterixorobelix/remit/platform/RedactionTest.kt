package com.asterixorobelix.remit.platform

import com.asterixorobelix.remit.testing.FakeAppConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RedactionTest {
    @Test
    fun personalDataAndCredentialsAreRemoved() {
        val cases = mapOf(
            "Could not send to bob.smith+test@example.co.uk" to "Could not send to [email]",
            "Authorization: Bearer abc.DEF-123_xyz" to "Authorization: [redacted] [redacted]",
            "token eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1MSJ9.c2ln expired" to "token [jwt] expired",
            "key $FAKE_API_KEY rejected" to "key [api-key] rejected",
            "GET /v1/accounts?key=s3cret&lang=en" to "GET /v1/accounts?key=[redacted]&lang=en",
            "password: hunter2, retries=3" to "password: [redacted], retries=3",
            """{"idToken":"abc123","uid":"u1"}""" to """{"idToken":"[redacted]","uid":"u1"}""",
            "X-Goog-Api-Key: abc123" to "X-Goog-Api-Key: [redacted]",
        )
        cases.forEach { (input, expected) -> assertEquals(expected, Redaction.redact(input), input) }
    }

    @Test
    fun ordinaryMessagesAreUnchanged() {
        listOf(
            "Payment history listener failed",
            "PERMISSION_DENIED: Missing or insufficient permissions.",
            "Unable to resolve host \"10.0.2.2\": No address associated with hostname",
            "monkey business",
        ).forEach { assertEquals(it, Redaction.redact(it)) }
    }

    @Test
    fun aCleanThrowableIsPassedOnAsItIs() {
        val error = IllegalStateException("Listener failed", RuntimeException("PERMISSION_DENIED"))
        assertSame(error, error.redacted())
    }

    @Test
    fun aThrowableIsRedactedWithItsCausesAndKeepsItsType() {
        val cause = RuntimeException("rejected token=abc123")
        val error = IllegalStateException("No account for bob@example.com", cause)

        val redacted = assertIs<RedactedException>(error.redacted())

        assertTrue(redacted.originalType.endsWith("IllegalStateException"), redacted.originalType)
        assertEquals("${redacted.originalType}: No account for [email]", redacted.message)
        assertFalse(redacted.cause?.message.orEmpty().contains("abc123"), redacted.cause?.message)
    }

    @Test
    fun theCrashLoggerRedactsEverythingItSends() {
        val sent = RecordingCrashLogger()
        val logger = RedactingCrashLogger(sent)

        logger.logException(IllegalStateException("bob@example.com"), "while sending to bob@example.com")
        logger.log("Bearer abc123")
        logger.setCustomKey("recipient", "bob@example.com")

        assertEquals(listOf("while sending to [email]", "Bearer [redacted]", "recipient=[email]"), sent.lines)
        assertTrue(sent.exceptions.single().message.orEmpty().endsWith("IllegalStateException: [email]"))
    }

    @Test
    fun releaseBuildsLogOnlyWarningsAndErrors() {
        assertEquals(LogLevel.DEBUG, FakeAppConfig(isDebug = true).minLogLevel())
        assertEquals(LogLevel.WARN, FakeAppConfig(isDebug = false).minLogLevel())
    }

    private companion object {
        /** Shaped like a Google API key; split so secret scanners do not flag the test. */
        const val FAKE_API_KEY = "AIza" + "SyA1234567890abcdefghijklmnopqrstuv"
    }

    private class RecordingCrashLogger : CrashLogger {
        val lines = mutableListOf<String>()
        val exceptions = mutableListOf<Throwable>()

        override fun initialize() = Unit

        override fun logException(throwable: Throwable, message: String?) {
            exceptions += throwable
            message?.let { lines += it }
        }

        override fun setCustomKey(key: String, value: String) {
            lines += "$key=$value"
        }

        override fun setUserId(userId: String) = Unit

        override fun log(message: String) {
            lines += message
        }

        override fun testCrash() = Unit
    }
}
