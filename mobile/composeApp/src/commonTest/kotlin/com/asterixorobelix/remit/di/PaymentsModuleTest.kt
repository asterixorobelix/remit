package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.network.NetworkConfig
import com.asterixorobelix.remit.data.network.createApiHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PaymentsModuleTest {
    @Test
    fun aPaymentIsRetriedOnceWithItsOwnTimeout() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = createApiHttpClient(
            config = NetworkConfig(),
            apiBaseUrl = { "https://api.test" },
            engine = MockEngine { request ->
                requests += request
                respond("", HttpStatusCode.ServiceUnavailable)
            },
        )

        client.post("/api/v1/payments") { paymentRequestOptions() }

        assertEquals(1 + PAYMENT_RETRY_POLICY.maxRetries, requests.size)
        assertEquals(
            PAYMENT_REQUEST_TIMEOUT_MS,
            requests.first().getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis,
        )
    }
}
