package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.network.NetworkConfig
import com.asterixorobelix.remit.data.network.createApiHttpClient
import com.asterixorobelix.remit.data.preferences.FakeAppPreferences
import com.asterixorobelix.remit.testing.FakeAppConfig
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
            engine = MockEngine { request ->
                requests += request
                respond("", HttpStatusCode.ServiceUnavailable)
            },
        )

        client.post("https://api.test/api/v1/payments") { paymentRequestOptions() }

        assertEquals(1 + PAYMENT_RETRY_POLICY.maxRetries, requests.size)
        assertEquals(
            PAYMENT_REQUEST_TIMEOUT_MS,
            requests.first().getCapabilityOrNull(HttpTimeoutCapability)?.requestTimeoutMillis,
        )
    }

    @Test
    fun onlyDebugBuildsUseTheDeveloperOverride() = runTest {
        val preferences = FakeAppPreferences().apply { setApiBaseUrlOverride("http://10.0.2.2:8085") }

        assertEquals("http://10.0.2.2:8085", FakeAppConfig(isDebug = true).resolveApiBaseUrl(preferences))
        assertEquals("https://api.test", FakeAppConfig(isDebug = false).resolveApiBaseUrl(preferences))
        assertEquals("https://api.test", FakeAppConfig(isDebug = true).resolveApiBaseUrl(FakeAppPreferences()))
    }
}
