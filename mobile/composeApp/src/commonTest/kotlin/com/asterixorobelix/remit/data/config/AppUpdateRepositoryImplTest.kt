package com.asterixorobelix.remit.data.config

import com.asterixorobelix.remit.data.network.ApiException
import com.asterixorobelix.remit.data.network.ApiPaths
import com.asterixorobelix.remit.data.network.NetworkConfig
import com.asterixorobelix.remit.data.network.RetryPolicy
import com.asterixorobelix.remit.data.network.createApiHttpClient
import com.asterixorobelix.remit.domain.update.VersionPolicy
import com.asterixorobelix.remit.shared.data.remote.ApiEndpoint
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AppUpdateRepositoryImplTest {
    private val requests = mutableListOf<HttpRequestData>()
    private var baseUrl = "https://api.test"

    private fun repository(status: HttpStatusCode, body: String = "") = AppUpdateRepositoryImpl(
        createApiHttpClient(
            config = NetworkConfig(retryPolicy = RetryPolicy.NO_RETRY),
            engine = MockEngine { request ->
                requests += request
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
        endpoint = ApiEndpoint { baseUrl },
    )

    @Test
    fun readsTheVersionPolicyFromTheConfigEndpoint() = runTest {
        val body = """{"minVersion":"1.1.0","latestVersion":"1.3.0","forceUpdate":true,"extra":1}"""

        val policy = repository(HttpStatusCode.OK, body).fetchVersionPolicy()

        assertEquals(VersionPolicy("1.1.0", "1.3.0", forceUpdate = true), policy.getOrThrow())
        assertEquals(ApiPaths.APP_CONFIG, requests.single().url.encodedPath)
    }

    @Test
    fun serverErrorsAreFailures() = runTest {
        val result = repository(HttpStatusCode.InternalServerError).fetchVersionPolicy()
        assertIs<ApiException>(result.exceptionOrNull())
    }

    @Test
    fun aChangedBaseUrlAppliesToTheNextRequest() = runTest {
        val repository = repository(HttpStatusCode.ServiceUnavailable)
        repository.fetchVersionPolicy()
        baseUrl = "http://10.0.2.2:8085/"
        repository.fetchVersionPolicy()

        assertEquals("https://api.test/api/v1/config", requests[0].url.toString())
        assertEquals("http://10.0.2.2:8085/api/v1/config", requests[1].url.toString())
    }
}
