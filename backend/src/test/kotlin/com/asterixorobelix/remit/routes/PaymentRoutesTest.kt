package com.asterixorobelix.remit.routes

import com.asterixorobelix.remit.body
import com.asterixorobelix.remit.error
import com.asterixorobelix.remit.postJson
import com.asterixorobelix.remit.service.InMemoryPaymentStore
import com.asterixorobelix.remit.service.PaymentService
import com.asterixorobelix.remit.service.TokenVerificationUnavailableException
import com.asterixorobelix.remit.service.TokenVerifier
import com.asterixorobelix.remit.startApp
import com.asterixorobelix.remit.testConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaymentRoutesTest {
    private val store = InMemoryPaymentStore()
    private var ids = 0
    private val tokens = TokenVerifier { token -> token.removePrefix("valid-").takeIf { token.startsWith("valid-") } }
    private val feature = PaymentsFeature(
        PaymentService(store, Clock.fixed(Instant.parse("2026-10-22T08:30:00Z"), ZoneOffset.UTC)) { "pay_${++ids}" },
        tokens,
    )
    private val valid = """{"recipientEmail":"bob@example.com","amount":10.50,"currency":"USD"}"""

    private fun paymentsTest(block: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
        startApp(testConfig(), feature)
        block()
    }

    private suspend fun HttpClient.pay(
        body: String = valid,
        user: String? = "alice",
        key: String? = "key-0000001",
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse = postJson("/api/v1/payments", body) {
        user?.let { bearerAuth("valid-$it") }
        key?.let { header(IDEMPOTENCY_KEY_HEADER, it) }
        block()
    }

    private suspend fun HttpResponse.code(): String? = body()["code"]?.jsonPrimitive?.content

    @Test
    fun createsAPaymentAndAnswersWithTheExactAmount() = paymentsTest {
        val response = client.pay()

        assertEquals(HttpStatusCode.Created, response.status)
        assertNull(response.headers[IDEMPOTENT_REPLAYED_HEADER])
        val text = response.bodyAsText()
        assertTrue(Regex(""""amount"\s*:\s*10\.50""").containsMatchIn(text), text)
        val body = response.body()
        assertEquals("pay_1", body.getValue("id").jsonPrimitive.content)
        assertEquals("bob@example.com", body.getValue("recipientEmail").jsonPrimitive.content)
        assertEquals("USD", body.getValue("currency").jsonPrimitive.content)
        assertEquals("COMPLETED", body.getValue("status").jsonPrimitive.content)
        assertEquals("2026-10-22T08:30:00Z", body.getValue("createdAt").jsonPrimitive.content)
        assertEquals("alice", store.payments.single().senderUid)
    }

    @Test
    fun replayReturnsTheOriginalPaymentAndSaysSo() = paymentsTest {
        client.pay()
        val replay = client.pay()

        assertEquals(HttpStatusCode.Created, replay.status)
        assertEquals("true", replay.headers[IDEMPOTENT_REPLAYED_HEADER])
        assertEquals("pay_1", replay.body().getValue("id").jsonPrimitive.content)
        assertEquals(1, store.payments.size)
    }

    @Test
    fun reusingAKeyForADifferentPaymentConflicts() = paymentsTest {
        client.pay()
        val conflict = client.pay(body = valid.replace("10.50", "11.00"))

        assertEquals(HttpStatusCode.Conflict, conflict.status)
        assertEquals("IDEMPOTENCY_KEY_REUSED", conflict.code())
    }

    @Test
    fun theSameKeyFromAnotherUserIsAnotherPayment() = paymentsTest {
        client.pay(user = "alice")
        assertEquals(HttpStatusCode.Created, client.pay(user = "bob").status)
        assertEquals(2, store.payments.size)
    }

    @Test
    fun requiresAValidFirebaseToken() = paymentsTest {
        val missing = client.pay(user = null)
        val wrongScheme = client.pay(user = null) { header("Authorization", "Basic dmFsaWQtYWxpY2U=") }
        val rejected = client.pay(user = null) { bearerAuth("forged") }

        listOf(missing, wrongScheme, rejected).forEach {
            assertEquals(HttpStatusCode.Unauthorized, it.status)
            assertEquals("UNAUTHENTICATED", it.code())
        }
        assertTrue(store.payments.isEmpty())
    }

    @Test
    fun requiresAWellFormedIdempotencyKey() = paymentsTest {
        assertEquals("IDEMPOTENCY_KEY_MISSING", client.pay(key = null).code())
        assertEquals("IDEMPOTENCY_KEY_MISSING", client.pay(key = " ").code())
        assertEquals("IDEMPOTENCY_KEY_INVALID", client.pay(key = "short").code())
        assertEquals("IDEMPOTENCY_KEY_INVALID", client.pay(key = "has spaces in it").code())
        assertEquals("IDEMPOTENCY_KEY_INVALID", client.pay(key = "k".repeat(256)).code())
        assertEquals(HttpStatusCode.Created, client.pay(key = UUID.randomUUID().toString()).status)
    }

    @Test
    fun invalidPaymentsListEveryProblem() = paymentsTest {
        val response = client.pay(body = """{"recipientEmail":"bob","amount":0.001,"currency":"USD"}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("VALIDATION_FAILED", response.code())
        assertEquals(
            listOf("AMOUNT_TOO_MANY_DECIMALS", "EMAIL_INVALID"),
            response.body().getValue("details").jsonArray.map { it.jsonPrimitive.content },
        )
        assertTrue(store.payments.isEmpty())
    }

    @Test
    fun missingFieldsAreReportedAsValidationCodes() = paymentsTest {
        val response = client.pay(body = "{}")
        assertEquals(
            listOf("AMOUNT_BLANK", "CURRENCY_MISSING", "EMAIL_BLANK"),
            response.body().getValue("details").jsonArray.map { it.jsonPrimitive.content },
        )
    }

    @Test
    fun floatingPointAndNonDecimalAmountsAreRejectedNotRounded() = paymentsTest {
        mapOf(
            "0.30000000000000004" to "AMOUNT_TOO_MANY_DECIMALS",
            "1e3" to "AMOUNT_NOT_A_NUMBER",
            "-5" to "AMOUNT_NOT_POSITIVE",
        )
            .forEach { (amount, code) ->
                val response = client.pay(body = valid.replace("10.50", amount), key = "key-$amount-x")
                assertEquals(
                    listOf(code),
                    response.body().getValue("details").jsonArray.map {
                        it.jsonPrimitive.content
                    },
                )
            }
    }

    @Test
    fun anAmountThatIsNotAJsonNumberIsAMalformedBody() = paymentsTest {
        listOf("\"10.50\"", "true", "[10.50]", "{}").forEach { amount ->
            val response = client.pay(body = valid.replace("10.50", amount))
            assertEquals(HttpStatusCode.BadRequest, response.status, amount)
            assertEquals("Malformed or missing request body", response.error())
        }
    }

    @Test
    fun aNullAmountIsReportedAsBlank() = paymentsTest {
        val response = client.pay(body = valid.replace("10.50", "null"))
        assertEquals(
            listOf("AMOUNT_BLANK"),
            response.body().getValue("details").jsonArray.map {
                it.jsonPrimitive.content
            },
        )
    }

    @Test
    fun aFailureToVerifyTokensIsUnavailableNotAnAuthError() = testApplication {
        val keysUnavailable = TokenVerifier {
            throw TokenVerificationUnavailableException(IllegalStateException("Could not fetch signing keys"))
        }
        startApp(testConfig(), PaymentsFeature(feature.service, keysUnavailable))
        val response = client.pay()
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("AUTH_UNAVAILABLE", response.code())
        assertTrue(store.payments.isEmpty())
    }

    @Test
    fun withoutFirebasePaymentsAreUnavailable() = testApplication {
        startApp(testConfig(), payments = null)
        val response = client.pay()
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertEquals("PAYMENTS_UNAVAILABLE", response.code())
    }
}
