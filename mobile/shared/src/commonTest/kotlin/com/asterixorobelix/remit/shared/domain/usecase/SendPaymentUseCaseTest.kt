package com.asterixorobelix.remit.shared.domain.usecase

import com.asterixorobelix.remit.shared.domain.repository.InMemoryPendingPaymentStore
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import com.asterixorobelix.remit.shared.testing.FakePaymentRepository
import com.asterixorobelix.remit.shared.testing.payment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SendPaymentUseCaseTest {
    private val draft = PaymentDraft("alice@example.com", "10.50", "USD")
    private var keyCounter = 0
    private val store = InMemoryPendingPaymentStore()

    private fun useCase(repository: FakePaymentRepository) =
        SendPaymentUseCase(PaymentValidator(), repository, IdempotencyKeys(store) { "key-${++keyCounter}" })

    @Test
    fun invalidDraftNeverReachesTheRepository() = runTest {
        val repository = FakePaymentRepository()
        val result = useCase(repository)(draft.copy(recipientEmail = "nope", amount = "0"))

        assertEquals(
            SendPaymentResult.Invalid(setOf(ValidationError.EMAIL_INVALID, ValidationError.AMOUNT_NOT_POSITIVE)),
            result,
        )
        assertTrue(repository.sends.isEmpty())
    }

    @Test
    fun validDraftIsSentWithAnIdempotencyKey() = runTest {
        val repository = FakePaymentRepository()
        val result = useCase(repository)(draft)

        assertIs<SendPaymentResult.Success>(result)
        assertEquals("key-1", repository.sends.single().second)
        assertEquals(1050, repository.sends.single().first.amount.minorUnits)
    }

    @Test
    fun retryAfterAFailureReusesTheKey() = runTest {
        val results = ArrayDeque(listOf(SendPaymentResult.Failed(PaymentFailure.TIMEOUT), null))
        val repository = FakePaymentRepository { request, _ ->
            results.removeFirst() ?: SendPaymentResult.Success(payment(amount = request.amount))
        }
        val send = useCase(repository)

        send(draft)
        send(draft)

        assertEquals(listOf("key-1", "key-1"), repository.sends.map { it.second })
    }

    @Test
    fun identicalDraftAfterSuccessIsAReplayUntilANewPaymentStarts() = runTest {
        val repository = FakePaymentRepository()
        val send = useCase(repository)

        send(draft)
        send(draft)
        send.startNewPayment()
        send(draft)

        assertEquals(listOf("key-1", "key-1", "key-2"), repository.sends.map { it.second })
    }

    @Test
    fun editedDraftGetsANewKey() = runTest {
        val repository = FakePaymentRepository { _, _ -> SendPaymentResult.Failed(PaymentFailure.NETWORK) }
        val send = useCase(repository)

        send(draft)
        send(draft.copy(amount = "10.51"))

        val keys = repository.sends.map { it.second }
        assertNotEquals(keys[0], keys[1])
    }

    @Test
    fun conflictDiscardsTheKey() = runTest {
        val repository = FakePaymentRepository { _, _ -> SendPaymentResult.Failed(PaymentFailure.IDEMPOTENCY_CONFLICT) }
        val send = useCase(repository)

        send(draft)
        send(draft)

        assertEquals(listOf("key-1", "key-2"), repository.sends.map { it.second })
    }

    @Test
    fun theKeyIsSavedBeforeTheRequestGoesOut() = runTest {
        var savedDuringSend: String? = null
        val repository = FakePaymentRepository { _, _ ->
            savedDuringSend = store.load()?.idempotencyKey
            SendPaymentResult.Failed(PaymentFailure.TIMEOUT)
        }
        useCase(repository)(draft)
        assertEquals("key-1", savedDuringSend)
    }

    @Test
    fun anUnfinishedPaymentSurvivesARestartAndKeepsItsKey() = runTest {
        val timeouts = FakePaymentRepository { _, _ -> SendPaymentResult.Failed(PaymentFailure.TIMEOUT) }
        useCase(timeouts)(draft)

        // A new use case over the same store is what the app gets after being killed and reopened.
        val afterRestartRepository = FakePaymentRepository()
        val afterRestart = useCase(afterRestartRepository)
        val unfinished = assertNotNull(afterRestart.unfinishedPayment())
        assertEquals("alice@example.com", unfinished.request.recipient.value)
        assertEquals(1050, unfinished.request.amount.minorUnits)

        assertIs<SendPaymentResult.Success>(afterRestart(draft))
        assertEquals("key-1", timeouts.sends.single().second)
        assertEquals("key-1", afterRestartRepository.sends.single().second)
    }

    @Test
    fun aPaymentTheServerRejectedIsNotLeftUnfinished() = runTest {
        val send =
            useCase(FakePaymentRepository { _, _ -> SendPaymentResult.Invalid(setOf(ValidationError.EMAIL_INVALID)) })
        send(draft)
        assertNull(send.unfinishedPayment())
    }

    @Test
    fun startingANewPaymentForgetsTheUnfinishedOne() = runTest {
        val send = useCase(FakePaymentRepository())
        send(draft)
        assertNotNull(send.unfinishedPayment())
        send.startNewPayment()
        assertNull(send.unfinishedPayment())
    }

    @Test
    fun aDoubleTapWaitsForTheFirstSendAndReusesItsKey() = runTest {
        val release = CompletableDeferred<Unit>()
        var inFlight = 0
        var maxInFlight = 0
        val repository = FakePaymentRepository { request, _ ->
            inFlight++
            maxInFlight = maxOf(maxInFlight, inFlight)
            release.await()
            inFlight--
            SendPaymentResult.Success(payment(amount = request.amount))
        }
        val send = useCase(repository)

        val taps = List(2) { async { send(draft) } }
        runCurrent() // first tap is now waiting on the server, second on the first
        assertEquals(1, repository.sends.size)
        release.complete(Unit)
        taps.awaitAll()

        assertEquals(1, maxInFlight)
        assertEquals(listOf("key-1", "key-1"), repository.sends.map { it.second })
    }
}
