package com.asterixorobelix.remit.shared.data.remote

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import com.asterixorobelix.remit.shared.testing.paymentRequest
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KtorPaymentsApiTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun api(respond: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) =
        KtorPaymentsApi(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    respond(request)
                },
            ),
            ApiEndpoint { "https://api.example.test/" },
        )

    private suspend fun send(api: KtorPaymentsApi) = api.createPayment(
        paymentRequest(amount = Money(1050, Currency.USD)),
        idempotencyKey = "key-123",
        idToken = "token-abc",
    )

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private val created = """
        {"id":"pay_9","recipientEmail":"alice@example.com","amount":10.50,"currency":"USD",
         "status":"COMPLETED","createdAt":"2026-10-22T08:30:00Z","processor":"mock"}
    """.trimIndent()

    @Test
    fun postsTheBriefsPayloadWithIdempotencyKeyAndToken() = runTest {
        send(api { json(HttpStatusCode.Created, created) })

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("https://api.example.test/api/v1/payments", request.url.toString())
        assertEquals("key-123", request.headers["Idempotency-Key"])
        assertEquals("Bearer token-abc", request.headers[HttpHeaders.Authorization])
        val body = request.body as TextContent
        assertEquals("application/json", body.contentType.toString())
        assertEquals("""{"recipientEmail":"alice@example.com","amount":10.50,"currency":"USD"}""", body.text)
    }

    @Test
    fun createdAndReplayedResponsesAreSuccesses() = runTest {
        listOf(HttpStatusCode.Created, HttpStatusCode.OK).forEach { status ->
            val result = send(api { json(status, created) })
            assertEquals("pay_9", assertIs<SendPaymentResult.Success>(result).payment.id)
            assertEquals(1050, result.payment.amount.minorUnits)
        }
    }

    @Test
    fun aReplayIsReportedAsSuch() = runTest {
        val replay = send(
            api {
                respond(
                    created,
                    HttpStatusCode.Created,
                    headersOf(
                        HttpHeaders.ContentType to listOf("application/json"),
                        "Idempotent-Replayed" to listOf("true"),
                    ),
                )
            },
        )
        val fresh = send(api { json(HttpStatusCode.Created, created) })
        assertEquals(true, assertIs<SendPaymentResult.Success>(replay).replayed)
        assertEquals(false, assertIs<SendPaymentResult.Success>(fresh).replayed)
    }

    @Test
    fun validationRejectionNamesTheFields() = runTest {
        val result = send(
            api {
                json(
                    HttpStatusCode.BadRequest,
                    """{"error":"Invalid payment","code":"VALIDATION_FAILED","details":["EMAIL_INVALID","NEW_RULE"]}""",
                )
            },
        )
        assertEquals(SendPaymentResult.Invalid(setOf(ValidationError.EMAIL_INVALID)), result)
    }

    @Test
    fun badRequestWithoutKnownFieldsIsUnexpected() = runTest {
        listOf(
            """{"error":"Idempotency-Key header is required","code":"IDEMPOTENCY_KEY_MISSING"}""",
            """{"error":"x","details":["NEW_RULE"]}""",
            "<html>Bad Request</html>",
        ).forEach { body ->
            assertEquals(
                failed(PaymentFailure.UNEXPECTED_RESPONSE),
                send(
                    api {
                        json(HttpStatusCode.BadRequest, body)
                    },
                ),
            )
        }
    }

    @Test
    fun statusCodesMapToFailures() = runTest {
        val expected = mapOf(
            HttpStatusCode.Unauthorized to PaymentFailure.UNAUTHORIZED,
            HttpStatusCode.Forbidden to PaymentFailure.UNAUTHORIZED,
            HttpStatusCode.Conflict to PaymentFailure.IDEMPOTENCY_CONFLICT,
            HttpStatusCode.TooManyRequests to PaymentFailure.RATE_LIMITED,
            HttpStatusCode.InternalServerError to PaymentFailure.SERVER,
            HttpStatusCode.ServiceUnavailable to PaymentFailure.SERVER,
            HttpStatusCode.NotFound to PaymentFailure.UNEXPECTED_RESPONSE,
            HttpStatusCode.Found to PaymentFailure.UNEXPECTED_RESPONSE,
        )
        expected.forEach { (status, failure) ->
            assertEquals(failed(failure), send(api { json(status, """{"error":"x"}""") }), status.toString())
        }
    }

    @Test
    fun malformedSuccessBodiesAreUnexpected() = runTest {
        listOf(
            "not json",
            """{"id":"pay_9"}""",
            created.replace("10.50", "\"10.50\""),
            created.replace("USD", "GBP"),
            "",
        ).forEach { body ->
            assertEquals(
                failed(PaymentFailure.UNEXPECTED_RESPONSE),
                send(
                    api {
                        json(HttpStatusCode.Created, body)
                    },
                ),
                body,
            )
        }
    }

    @Test
    fun timeoutsAndConnectionErrorsAreRetryableFailures() = runTest {
        val timeout = send(api { throw HttpRequestTimeoutException("https://api.example.test", 15_000) })
        val offline = send(api { throw IOException("Network is unreachable") })

        assertEquals(failed(PaymentFailure.TIMEOUT), timeout)
        assertEquals(failed(PaymentFailure.NETWORK), offline)
        assertEquals(true, PaymentFailure.TIMEOUT.retryable && PaymentFailure.NETWORK.retryable)
    }

    @Test
    fun hostAppCanConfigureThePaymentRequest() = runTest {
        val api = KtorPaymentsApi(
            HttpClient(
                MockEngine { request ->
                    requests += request
                    json(HttpStatusCode.Created, created)
                },
            ),
            ApiEndpoint { "https://api.example.test" },
            PaymentRequestConfigurer { it.headers.append("X-Retry-Allowed", "true") },
        )
        send(api)
        assertEquals("true", requests.single().headers["X-Retry-Allowed"])
        assertEquals("https://api.example.test/api/v1/payments", requests.single().url.toString())
    }

    private fun failed(reason: PaymentFailure) = SendPaymentResult.Failed(reason)
}
