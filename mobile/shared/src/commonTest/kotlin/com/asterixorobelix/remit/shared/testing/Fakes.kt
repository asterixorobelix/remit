package com.asterixorobelix.remit.shared.testing

import com.asterixorobelix.remit.shared.data.auth.AuthSession
import com.asterixorobelix.remit.shared.data.auth.AuthUnavailableException
import com.asterixorobelix.remit.shared.data.history.PaymentHistorySource
import com.asterixorobelix.remit.shared.data.history.PaymentRecord
import com.asterixorobelix.remit.shared.data.remote.PaymentsApi
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

fun email(value: String): EmailAddress = requireNotNull(EmailAddress.parse(value)) { "Invalid test email $value" }

fun usd(minorUnits: Long) = Money(minorUnits, Currency.USD)

fun paymentRequest(recipient: String = "alice@example.com", amount: Money = usd(1050)) =
    PaymentRequest(email(recipient), amount)

fun payment(
    id: String = "pay_1",
    recipient: String = "alice@example.com",
    amount: Money = usd(1050),
    createdAtMillis: Long = 1_760_000_000_000,
) = Payment(id, email(recipient), amount, PaymentStatus.COMPLETED, Instant.fromEpochMilliseconds(createdAtMillis))

data class ApiCall(val request: PaymentRequest, val idempotencyKey: String, val idToken: String)

/** Answers with [results] in order (the last one repeats) and records every call. */
class FakePaymentsApi(vararg results: SendPaymentResult) : PaymentsApi {
    private val queue = results.toMutableList()
    val calls = mutableListOf<ApiCall>()

    override suspend fun createPayment(
        request: PaymentRequest,
        idempotencyKey: String,
        idToken: String,
    ): SendPaymentResult {
        calls += ApiCall(request, idempotencyKey, idToken)
        return if (queue.size > 1) queue.removeAt(0) else queue.single()
    }
}

class FakeAuthSession(
    private val uid: String = "user-1",
    var failure: AuthUnavailableException? = null,
) : AuthSession {
    var tokenVersion = 1
        private set

    override suspend fun userId(): String = failure?.let { throw it } ?: uid

    override suspend fun idToken(forceRefresh: Boolean): String {
        failure?.let { throw it }
        if (forceRefresh) tokenVersion++
        return "token-$tokenVersion"
    }
}

class FakeHistorySource : PaymentHistorySource {
    val records = MutableSharedFlow<List<PaymentRecord>>(replay = 1)
    val observedUserIds = mutableListOf<String>()

    override fun observe(userId: String): Flow<List<PaymentRecord>> {
        observedUserIds += userId
        return records
    }
}

/** Records sends; [onSend] decides the result. */
class FakePaymentRepository(
    private val onSend: suspend (PaymentRequest, String) -> SendPaymentResult = { request, _ ->
        SendPaymentResult.Success(payment(recipient = request.recipient.value, amount = request.amount))
    },
) : PaymentRepository {
    val sends = mutableListOf<Pair<PaymentRequest, String>>()
    val history = MutableSharedFlow<List<Payment>>(replay = 1)

    override suspend fun send(request: PaymentRequest, idempotencyKey: String): SendPaymentResult {
        sends += request to idempotencyKey
        return onSend(request, idempotencyKey)
    }

    override fun observeHistory(): Flow<List<Payment>> = history
}
