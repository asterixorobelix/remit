package com.asterixorobelix.remit.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ApiHttpClientTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun client(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = createApiHttpClient(
        config = NetworkConfig(retryPolicy = RetryPolicy(maxRetries = 2, initialDelayMs = 1, maxDelayMs = 1)),
        engine = MockEngine { request ->
            requests += request
            handler(request)
        },
    )

    @Test
    fun everyRequestGetsItsOwnRequestId() = runTest {
        val client = client { respondOk() }
        client.get(CONFIG_URL)
        client.get(CONFIG_URL)
        val ids = requests.map { it.headers[HttpHeaders.XRequestId] }
        assertEquals(2, ids.filterNotNull().size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun onlyIdempotentRequestsAreRetried() = runTest {
        val client = client { respond("", HttpStatusCode.ServiceUnavailable) }

        client.post(EXAMPLE_URL) { setBody("{}") }
        assertEquals(1, requests.size)

        client.get(CONFIG_URL)
        assertEquals(1 + 3, requests.size)
    }

    @Test
    fun allowRetryOptsAPostIntoRetries() = runTest {
        val client = client { respond("", HttpStatusCode.ServiceUnavailable) }

        client.post(EXAMPLE_URL) {
            allowRetry()
            setBody("{}")
        }

        assertEquals(1 + 2, requests.size)
        assertEquals(1, requests.map { it.headers[HttpHeaders.XRequestId] }.distinct().size)
    }

    @Test
    fun aPolicyGivenToAllowRetryReplacesTheClientPolicy() = runTest {
        val client = client { respond("", HttpStatusCode.ServiceUnavailable) }

        client.post(EXAMPLE_URL) {
            allowRetry(RetryPolicy(maxRetries = 1, initialDelayMs = 1, maxDelayMs = 1))
            setBody("{}")
        }

        assertEquals(1 + 1, requests.size)
    }

    @Test
    fun aRetryAfterLongerThanThePolicyWaitsEndsTheRetries() = runTest {
        val client = client {
            respond("", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "60"))
        }

        val response = client.get(CONFIG_URL)

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals(1, requests.size)
    }

    @Test
    fun aRetryAfterWithinThePolicyIsWaitedOutAndRetried() = runTest {
        val client = client {
            respond("", HttpStatusCode.TooManyRequests, headersOf(HttpHeaders.RetryAfter, "0"))
        }

        client.get(CONFIG_URL)

        assertEquals(1 + 2, requests.size)
    }

    private companion object {
        const val CONFIG_URL = "https://api.test/api/v1/config"
        const val EXAMPLE_URL = "https://api.test/api/v1/example"
    }
}
