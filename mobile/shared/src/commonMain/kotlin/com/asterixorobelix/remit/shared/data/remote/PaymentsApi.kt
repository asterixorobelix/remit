package com.asterixorobelix.remit.shared.data.remote

import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Base URL of the payments API, resolved per request so a debug override applies at once. */
fun interface ApiEndpoint {
    fun baseUrl(): String
}

/**
 * Lets the host app add its own request options, such as opting the payment POST in to automatic
 * retries: safe here because every attempt carries the same idempotency key.
 */
fun interface PaymentRequestConfigurer {
    fun configure(request: HttpRequestBuilder)
}

interface PaymentsApi {
    suspend fun createPayment(request: PaymentRequest, idempotencyKey: String, idToken: String): SendPaymentResult
}

/**
 * `POST /api/v1/payments` over any Ktor [HttpClient]. It encodes and decodes JSON itself, so it does
 * not depend on how the host app configured content negotiation, and it maps every outcome
 * (status codes, timeouts, offline, malformed bodies) to a [SendPaymentResult] instead of throwing.
 */
class KtorPaymentsApi(
    private val client: HttpClient,
    private val endpoint: ApiEndpoint,
    private val configurer: PaymentRequestConfigurer = PaymentRequestConfigurer { },
) : PaymentsApi {
    override suspend fun createPayment(
        request: PaymentRequest,
        idempotencyKey: String,
        idToken: String,
    ): SendPaymentResult = try {
        client.post("${endpoint.baseUrl().trimEnd('/')}$PAYMENTS_PATH") {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            bearerAuth(idToken)
            setBody(TextContent(json.encodeToString(request.toDto()), ContentType.Application.Json))
            configurer.configure(this)
        }.toResult()
    } catch (expected: HttpRequestTimeoutException) {
        SendPaymentResult.Failed(PaymentFailure.TIMEOUT)
    } catch (expected: IOException) {
        SendPaymentResult.Failed(PaymentFailure.NETWORK)
    }

    private suspend fun HttpResponse.toResult(): SendPaymentResult = when {
        status.isSuccess() -> decodeOrNull<PaymentResponseDto>(bodyAsText())?.toDomainOrNull()
            ?.let(SendPaymentResult::Success)
            ?: SendPaymentResult.Failed(PaymentFailure.UNEXPECTED_RESPONSE)

        status == HttpStatusCode.BadRequest -> rejection(bodyAsText())

        status == HttpStatusCode.Unauthorized || status == HttpStatusCode.Forbidden ->
            SendPaymentResult.Failed(PaymentFailure.UNAUTHORIZED)

        status == HttpStatusCode.Conflict -> SendPaymentResult.Failed(PaymentFailure.IDEMPOTENCY_CONFLICT)

        status == HttpStatusCode.TooManyRequests -> SendPaymentResult.Failed(PaymentFailure.RATE_LIMITED)

        status.value >= HttpStatusCode.InternalServerError.value -> SendPaymentResult.Failed(PaymentFailure.SERVER)

        else -> SendPaymentResult.Failed(PaymentFailure.UNEXPECTED_RESPONSE)
    }

    /** A 400 is a validation rejection only when it names fields this app knows. */
    private fun rejection(body: String): SendPaymentResult {
        val errors = decodeOrNull<ErrorResponseDto>(body)?.details.orEmpty()
            .mapNotNull(ValidationError::fromCode)
            .toSet()
        return if (errors.isEmpty()) {
            SendPaymentResult.Failed(PaymentFailure.UNEXPECTED_RESPONSE)
        } else {
            SendPaymentResult.Invalid(errors)
        }
    }

    private inline fun <reified T> decodeOrNull(body: String): T? = try {
        json.decodeFromString<T>(body)
    } catch (expected: SerializationException) {
        null
    } catch (expected: IllegalArgumentException) {
        null
    }

    companion object {
        const val PAYMENTS_PATH = "/api/v1/payments"
        const val IDEMPOTENCY_KEY_HEADER = "Idempotency-Key"

        /** Unknown fields are ignored so the server can add fields without breaking older apps. */
        internal val json = Json { ignoreUnknownKeys = true }
    }
}
