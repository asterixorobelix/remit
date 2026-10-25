package com.asterixorobelix.remit.shared.data

import app.cash.turbine.test
import com.asterixorobelix.remit.shared.data.auth.AuthUnavailableException
import com.asterixorobelix.remit.shared.data.auth.UnsupportedPlatformAuthSession
import com.asterixorobelix.remit.shared.data.history.PaymentHistoryUnavailableException
import com.asterixorobelix.remit.shared.data.history.UnsupportedPlatformHistorySource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Builds without a backend fail clearly instead of pretending to have data. */
class UnsupportedPlatformTest {
    @Test
    fun authFailsWithAClearReason() = runTest {
        val error = assertFailsWith<AuthUnavailableException> { UnsupportedPlatformAuthSession.userId() }
        assertIs<BackendNotSetUpException>(error.cause)
        assertTrue(error.isNotSetUp)
        assertFailsWith<AuthUnavailableException> { UnsupportedPlatformAuthSession.idToken(forceRefresh = false) }
    }

    @Test
    fun historyFailsWithAClearReason() = runTest {
        UnsupportedPlatformHistorySource.observe("user").test {
            val error = assertIs<PaymentHistoryUnavailableException>(awaitError())
            assertIs<BackendNotSetUpException>(error.cause)
        }
    }
}
