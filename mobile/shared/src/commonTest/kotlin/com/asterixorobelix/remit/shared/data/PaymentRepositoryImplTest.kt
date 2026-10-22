package com.asterixorobelix.remit.shared.data

import app.cash.turbine.test
import com.asterixorobelix.remit.shared.data.auth.AuthUnavailableException
import com.asterixorobelix.remit.shared.data.history.PaymentRecord
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.testing.FakeAuthSession
import com.asterixorobelix.remit.shared.testing.FakeHistorySource
import com.asterixorobelix.remit.shared.testing.FakePaymentsApi
import com.asterixorobelix.remit.shared.testing.payment
import com.asterixorobelix.remit.shared.testing.paymentRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PaymentRepositoryImplTest {
    private val auth = FakeAuthSession(uid = "user-42")
    private val history = FakeHistorySource()
    private val success = SendPaymentResult.Success(payment())
    private val unauthorized = SendPaymentResult.Failed(PaymentFailure.UNAUTHORIZED)

    private fun repository(api: FakePaymentsApi) = PaymentRepositoryImpl(api, history, auth)

    @Test
    fun sendsWithTheCurrentIdToken() = runTest {
        val api = FakePaymentsApi(success)
        assertEquals(success, repository(api).send(paymentRequest(), "key-1"))
        assertEquals("token-1", api.calls.single().idToken)
        assertEquals("key-1", api.calls.single().idempotencyKey)
    }

    @Test
    fun anExpiredTokenIsRefreshedOnceUnderTheSameKey() = runTest {
        val api = FakePaymentsApi(unauthorized, success)
        assertEquals(success, repository(api).send(paymentRequest(), "key-1"))
        assertEquals(listOf("token-1", "token-2"), api.calls.map { it.idToken })
        assertEquals(listOf("key-1", "key-1"), api.calls.map { it.idempotencyKey })
    }

    @Test
    fun aSecondRejectionIsReturned() = runTest {
        val api = FakePaymentsApi(unauthorized)
        assertEquals(unauthorized, repository(api).send(paymentRequest(), "key-1"))
        assertEquals(2, api.calls.size)
    }

    @Test
    fun otherFailuresAreNotRetriedHere() = runTest {
        val api = FakePaymentsApi(SendPaymentResult.Failed(PaymentFailure.SERVER))
        repository(api).send(paymentRequest(), "key-1")
        assertEquals(1, api.calls.size)
    }

    @Test
    fun signInFailuresBecomePaymentFailures() = runTest {
        val api = FakePaymentsApi(success)
        auth.failure = AuthUnavailableException(isNetworkError = true)
        assertEquals(SendPaymentResult.Failed(PaymentFailure.NETWORK), repository(api).send(paymentRequest(), "k"))
        auth.failure = AuthUnavailableException(isNetworkError = false)
        assertEquals(unauthorized, repository(api).send(paymentRequest(), "k"))
        assertTrue(api.calls.isEmpty())
    }

    @Test
    fun historyIsTheSignedInUsersNewestFirstWithBadRecordsSkipped() = runTest {
        repository(FakePaymentsApi(success)).observeHistory().test {
            history.records.emit(
                listOf(
                    record("old", createdAt = 1_000),
                    record("broken", createdAt = 3_000).copy(amountMinor = null),
                    record("new", createdAt = 2_000),
                ),
            )
            assertEquals(listOf("new", "old"), awaitItem().map { it.id })
            history.records.emit(emptyList())
            assertEquals(emptyList(), awaitItem())
        }
        assertEquals(listOf("user-42"), history.observedUserIds)
    }

    @Test
    fun historyFailsWhenSignInFails() = runTest {
        auth.failure = AuthUnavailableException(isNetworkError = true)
        repository(FakePaymentsApi(success)).observeHistory().test {
            assertIs<AuthUnavailableException>(awaitError())
        }
    }

    private fun record(id: String, createdAt: Long) = PaymentRecord(
        id = id,
        recipientEmail = "alice@example.com",
        amountMinor = 500,
        currency = "USD",
        status = "COMPLETED",
        createdAtEpochMillis = createdAt,
    )
}
