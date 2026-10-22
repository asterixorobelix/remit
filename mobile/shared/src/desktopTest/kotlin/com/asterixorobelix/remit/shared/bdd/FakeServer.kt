package com.asterixorobelix.remit.shared.bdd

import com.asterixorobelix.remit.shared.data.auth.AuthSession
import com.asterixorobelix.remit.shared.data.history.PaymentHistorySource
import com.asterixorobelix.remit.shared.data.history.PaymentRecord
import com.asterixorobelix.remit.shared.data.remote.CreatePaymentRequestDto
import com.asterixorobelix.remit.shared.data.remote.ErrorResponseDto
import com.asterixorobelix.remit.shared.data.remote.KtorPaymentsApi
import com.asterixorobelix.remit.shared.data.remote.PaymentResponseDto
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import com.asterixorobelix.remit.shared.domain.validation.ValidationResult
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlin.time.Instant

/** A stored payment, as the backend writes it to `payments/{id}`. */
data class StoredPayment(val senderUid: String, val record: PaymentRecord)

/** Firestore for the scenarios: a live collection, queried per user like the real listener. */
class FakeFirestore : PaymentHistorySource {
    private val payments = MutableStateFlow<List<StoredPayment>>(emptyList())
    private var clock = START_MILLIS

    val all: List<StoredPayment> get() = payments.value

    fun record(senderUid: String, recipientEmail: String, amountMinor: Long, currency: String): PaymentRecord {
        clock += STEP_MILLIS
        val record = PaymentRecord(
            id = "pay_${payments.value.size + 1}",
            recipientEmail = recipientEmail,
            amountMinor = amountMinor,
            currency = currency,
            status = "COMPLETED",
            createdAtEpochMillis = clock,
        )
        payments.update { it + StoredPayment(senderUid, record) }
        return record
    }

    override fun observe(userId: String): Flow<List<PaymentRecord>> = payments.map { stored ->
        stored.filter { it.senderUid == userId }.map { it.record }
    }

    private companion object {
        const val START_MILLIS = 1_792_657_800_000
        const val STEP_MILLIS = 1_000L
    }
}

/** The signed-in user; the token names the user so the fake backend can tell who is calling. */
class TestAuthSession(var uid: String = "user-a") : AuthSession {
    override suspend fun userId(): String = uid

    override suspend fun idToken(forceRefresh: Boolean): String = "token-$uid"
}

/**
 * The payments API as an HTTP fake behind Ktor's MockEngine, so scenarios exercise the real client:
 * JSON with an exact amount, the Idempotency-Key header, and status code handling. Like the real
 * backend it validates, records each key once per user (a replay of the same payment returns the
 * original; a different payment under that key is 409), and writes the payment to Firestore.
 */
class FakePaymentsBackend(private val firestore: FakeFirestore) {
    private val json = Json { ignoreUnknownKeys = true }

    /** Per (user, key): the payment's fingerprint, as the server compares it, and the response. */
    private val processed = mutableMapOf<Pair<String, String>, Pair<Fingerprint, PaymentResponseDto>>()

    val requests = mutableListOf<Pair<String, CreatePaymentRequestDto>>()
    var timeoutAfterProcessingOnce = false

    fun handle(scope: MockRequestHandleScope, request: HttpRequestData): HttpResponseData {
        val uid = request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer token-").orEmpty()
        val key = request.headers[KtorPaymentsApi.IDEMPOTENCY_KEY_HEADER].orEmpty()
        val dto = json.decodeFromString<CreatePaymentRequestDto>((request.body as TextContent).text)
        requests += key to dto
        val response = when (
            val validation = PaymentValidator().validate(PaymentDraft(dto.recipientEmail, dto.amount, dto.currency))
        ) {
            is ValidationResult.Invalid -> {
                val error = ErrorResponseDto("Invalid payment", "VALIDATION_FAILED", validation.errors.map { it.name })
                scope.json(HttpStatusCode.BadRequest, json.encodeToString(error))
            }

            is ValidationResult.Valid -> record(scope, uid, key, validation.request)
        }
        if (timeoutAfterProcessingOnce) {
            timeoutAfterProcessingOnce = false
            throw HttpRequestTimeoutException(request.url.toString(), TIMEOUT_MILLIS)
        }
        return response
    }

    private fun record(
        scope: MockRequestHandleScope,
        uid: String,
        key: String,
        request: PaymentRequest,
    ): HttpResponseData {
        val fingerprint = Fingerprint(request.recipient.value, request.amount.minorUnits, request.amount.currency.code)
        processed[uid to key]?.let { (original, payment) ->
            return if (original == fingerprint) {
                scope.json(HttpStatusCode.Created, json.encodeToString(payment), replayed = true)
            } else {
                val error = ErrorResponseDto("Idempotency-Key was already used", "IDEMPOTENCY_KEY_REUSED")
                scope.json(HttpStatusCode.Conflict, json.encodeToString(error))
            }
        }
        val record = firestore.record(uid, fingerprint.recipient, fingerprint.amountMinor, fingerprint.currency)
        val payment = PaymentResponseDto(
            id = record.id,
            recipientEmail = request.recipient.value,
            amount = request.amount.toDecimalString(),
            currency = request.amount.currency.code,
            status = "COMPLETED",
            createdAt = Instant.fromEpochMilliseconds(checkNotNull(record.createdAtEpochMillis)).toString(),
        )
        processed[uid to key] = fingerprint to payment
        return scope.json(HttpStatusCode.Created, json.encodeToString(payment))
    }

    /** What makes two requests the same payment on the server: normalized recipient, cents, currency. */
    private data class Fingerprint(val recipient: String, val amountMinor: Long, val currency: String)

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String, replayed: Boolean = false) =
        respond(
            body,
            status,
            headersOf(
                HttpHeaders.ContentType to listOf("application/json"),
                KtorPaymentsApi.IDEMPOTENT_REPLAYED_HEADER to listOf(replayed.toString()),
            ),
        )

    private companion object {
        const val TIMEOUT_MILLIS = 15_000L
    }
}
