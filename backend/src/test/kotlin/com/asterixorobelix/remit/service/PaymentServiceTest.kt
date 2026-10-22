package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentStatus
import com.asterixorobelix.remit.domain.StoredPayment
import com.asterixorobelix.remit.dto.CreatePaymentRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PaymentServiceTest {
    private val store = InMemoryPaymentStore()
    private var ids = 0
    private val clock = Clock.fixed(Instant.parse("2026-10-22T08:30:00.123456789Z"), ZoneOffset.UTC)
    private val service = PaymentService(store, clock) { "pay_${++ids}" }
    private val request = CreatePaymentRequest("bob@example.com", "10.50", "USD")

    @Test
    fun createsACompletedPaymentInCents() = runTest {
        val payment = assertIs<PaymentResult.Created>(service.create("alice", "key-00001", request)).payment
        assertEquals("pay_1", payment.id)
        assertEquals("alice", payment.senderUid)
        assertEquals(1050, payment.amountMinor)
        assertEquals(Currency.USD, payment.currency)
        assertEquals(PaymentStatus.COMPLETED, payment.status)
        assertEquals(Instant.parse("2026-10-22T08:30:00.123Z"), payment.createdAt)
    }

    @Test
    fun sameKeyAndPaymentReturnsTheOriginal() = runTest {
        service.create("alice", "key-00001", request)
        val replay = assertIs<PaymentResult.Replayed>(service.create("alice", "key-00001", request))
        assertEquals("pay_1", replay.payment.id)
        assertEquals(1, store.payments.size)
    }

    @Test
    fun theSamePaymentSpelledDifferentlyIsStillAReplay() = runTest {
        service.create("alice", "key-00001", request)
        val respelled = CreatePaymentRequest(" BOB@example.com", "10.5", "USD")
        assertIs<PaymentResult.Replayed>(service.create("alice", "key-00001", respelled))
    }

    @Test
    fun sameKeyWithADifferentPaymentIsRejected() = runTest {
        service.create("alice", "key-00001", request)
        assertEquals(PaymentResult.KeyReused, service.create("alice", "key-00001", request.copy(amount = "10.51")))
        assertEquals(PaymentResult.KeyReused, service.create("alice", "key-00001", request.copy(currency = "EUR")))
        assertEquals(1, store.payments.size)
    }

    @Test
    fun anUnreadableOriginalIsReportedNotRecreated() = runTest {
        val unreadable = object : PaymentStore {
            override suspend fun createOnce(idempotencyKey: String, fingerprint: String, payment: StoredPayment) =
                CreateOutcome.ReplayUnavailable("pay_0")
        }
        val result = PaymentService(unreadable, clock).create("alice", "key-00001", request)
        assertEquals(PaymentResult.ReplayUnavailable("pay_0"), result)
    }

    @Test
    fun keysBelongToOneUser() = runTest {
        service.create("alice", "key-00001", request)
        assertIs<PaymentResult.Created>(service.create("bob", "key-00001", request))
        assertEquals(2, store.payments.size)
    }

    @Test
    fun invalidRequestsNeverReachTheStore() = runTest {
        val result = service.create("alice", "key-00001", CreatePaymentRequest("nope", "0", "GBP"))
        assertEquals(
            PaymentResult.Invalid(
                setOf(
                    ValidationCode.EMAIL_INVALID,
                    ValidationCode.AMOUNT_NOT_POSITIVE,
                    ValidationCode.CURRENCY_UNSUPPORTED,
                ),
            ),
            result,
        )
        assertEquals(0, store.payments.size)
    }

    @Test
    fun concurrentRetriesProduceOnePayment() = runTest {
        val results = List(20) { async { service.create("alice", "key-00001", request) } }.awaitAll()
        assertEquals(1, results.count { it is PaymentResult.Created })
        assertEquals(19, results.count { it is PaymentResult.Replayed })
        assertEquals(1, store.payments.size)
    }

    @Test
    fun defaultIdsArePrefixedAndUnique() = runTest {
        val defaults = PaymentService(InMemoryPaymentStore())
        val first = assertIs<PaymentResult.Created>(defaults.create("a", "key-00001", request)).payment.id
        val second = assertIs<PaymentResult.Created>(defaults.create("a", "key-00002", request)).payment.id
        assertEquals(true, first.startsWith("pay_") && first != second)
    }
}
