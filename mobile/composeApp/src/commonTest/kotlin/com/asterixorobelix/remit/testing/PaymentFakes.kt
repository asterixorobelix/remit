package com.asterixorobelix.remit.testing

import com.asterixorobelix.remit.platform.CrashLogger
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.time.Instant

fun testPayment(
    id: String = "pay_1",
    recipient: String = "bob@example.com",
    minorUnits: Long = 1050,
    currency: Currency = Currency.USD,
    createdAtMillis: Long = 1_792_657_800_000,
) = Payment(
    id = id,
    recipient = requireNotNull(EmailAddress.parse(recipient)),
    amount = Money(minorUnits, currency),
    status = PaymentStatus.COMPLETED,
    createdAt = Instant.fromEpochMilliseconds(createdAtMillis),
)

/** [results] answer sends in order; the last one repeats. History is driven through [history]. */
class FakePaymentRepository(vararg results: SendPaymentResult) : PaymentRepository {
    private val queue = results.toMutableList()
    val sends = mutableListOf<Pair<PaymentRequest, String>>()
    val history = MutableSharedFlow<List<Payment>>(replay = 1)
    var historyCollections = 0
        private set

    override suspend fun send(request: PaymentRequest, idempotencyKey: String): SendPaymentResult {
        sends += request to idempotencyKey
        return if (queue.size > 1) queue.removeAt(0) else queue.single()
    }

    override fun observeHistory(): Flow<List<Payment>> {
        historyCollections++
        return history
    }
}

class RecordingCrashLogger : CrashLogger {
    val exceptions = mutableListOf<Throwable>()

    override fun initialize() = Unit

    override fun logException(throwable: Throwable, message: String?) {
        exceptions += throwable
    }

    override fun setCustomKey(key: String, value: String) = Unit

    override fun setUserId(userId: String) = Unit

    override fun log(message: String) = Unit

    override fun testCrash() = Unit
}
