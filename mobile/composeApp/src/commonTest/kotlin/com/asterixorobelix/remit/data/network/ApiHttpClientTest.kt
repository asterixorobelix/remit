package com.asterixorobelix.remit.data.network

import com.asterixorobelix.remit.data.auth.AuthApi
import com.asterixorobelix.remit.data.auth.AuthRepositoryImpl
import com.asterixorobelix.remit.data.preferences.FakeAppPreferences
import com.asterixorobelix.remit.data.session.SessionEvents
import com.asterixorobelix.remit.data.session.SessionTokens
import com.asterixorobelix.remit.data.session.TokenStore
import com.asterixorobelix.remit.testing.FakeProfileCache
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
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiHttpClientTest {
    private val preferences = FakeAppPreferences()
    private val tokenStore = TokenStore(preferences)
    private val sessionEvents = SessionEvents()
    private val requests = mutableListOf<HttpRequestData>()
    private var baseUrl = "https://api.test"

    private fun client(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): HttpClient = createApiHttpClient(
        config = NetworkConfig(retryPolicy = RetryPolicy(maxRetries = 2, initialDelayMs = 1, maxDelayMs = 1)),
        apiBaseUrl = { baseUrl },
        tokenStore = tokenStore,
        sessionEvents = sessionEvents,
        engine = MockEngine { request ->
            requests += request
            handler(request)
        },
    )

    private fun MockRequestHandleScope.json(body: String, status: HttpStatusCode = HttpStatusCode.OK) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun authJson(access: String, refresh: String) =
        """{"accessToken":"$access","refreshToken":"$refresh","user":{"id":1,"email":"a@b.co","displayName":"A"}}"""

    private fun HttpRequestData.bearer(): String? = headers[HttpHeaders.Authorization]

    @Test
    fun everyRequestGetsItsOwnRequestId() = runTest {
        val client = client { respondOk() }
        client.get("/api/v1/config")
        client.get("/api/v1/config")
        val ids = requests.map { it.headers[HttpHeaders.XRequestId] }
        assertEquals(2, ids.filterNotNull().size)
        assertNotEquals(ids[0], ids[1])
    }

    @Test
    fun bearerTokenIsOnlySentToTheApiOrigin() = runTest {
        tokenStore.save(SessionTokens("a1", "r1"))
        val client = client { respondOk() }

        client.get(ApiPaths.CURRENT_USER)
        client.get("https://third-party.test/image.png")
        client.post(ApiPaths.LOGIN)

        assertEquals("Bearer a1", requests[0].bearer())
        assertNull(requests[1].bearer())
        assertNull(requests[2].bearer())
    }

    @Test
    fun rotatedRefreshTokenReplacesTheStoredOne() = runTest {
        tokenStore.save(SessionTokens("a1", "r1"))
        val client = client { request ->
            when {
                request.url.encodedPath == ApiPaths.REFRESH -> json(authJson("a2", "r2"))
                request.bearer() == "Bearer a2" -> respondOk()
                else -> respond("", HttpStatusCode.Unauthorized)
            }
        }

        val response = client.get(ApiPaths.CURRENT_USER)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("a2", preferences.getAccessToken())
        assertEquals("r2", preferences.getRefreshToken())
        val refreshBody = (requests.single { it.url.encodedPath == ApiPaths.REFRESH }.body as TextContent).text
        assertTrue("\"r1\"" in refreshBody)
    }

    @Test
    fun rejectedRefreshClearsSessionAndSignalsExpiry() = runTest {
        tokenStore.save(SessionTokens("a1", "r1"))
        val client = client { respond("", HttpStatusCode.Unauthorized) }

        val expired = async(start = CoroutineStart.UNDISPATCHED) { sessionEvents.expired.first() }
        client.get(ApiPaths.CURRENT_USER)

        expired.await()
        assertNull(preferences.getRefreshToken())
        assertNull(preferences.getAccessToken())
    }

    @Test
    fun rateLimitedOrFailingRefreshKeepsTheSession() = runTest {
        tokenStore.save(SessionTokens("a1", "r1"))
        val client = client { request ->
            if (request.url.encodedPath == ApiPaths.REFRESH) {
                respond("", HttpStatusCode.TooManyRequests)
            } else {
                respond("", HttpStatusCode.Unauthorized)
            }
        }

        client.get(ApiPaths.CURRENT_USER)

        assertEquals("r1", preferences.getRefreshToken())
        assertEquals(1, requests.count { it.url.encodedPath == ApiPaths.REFRESH })
    }

    @Test
    fun onlyIdempotentRequestsAreRetried() = runTest {
        val client = client { respond("", HttpStatusCode.ServiceUnavailable) }

        client.post(ApiPaths.TASKS_SYNC) { setBody("{}") }
        assertEquals(1, requests.size)

        client.get(ApiPaths.APP_CONFIG)
        assertEquals(1 + 3, requests.size)
    }

    @Test
    fun baseUrlOverrideAppliesToTheNextRequest() = runTest {
        val client = client { respondOk() }
        client.get(ApiPaths.APP_CONFIG)
        baseUrl = "http://10.0.2.2:8085"
        client.get(ApiPaths.APP_CONFIG)
        assertEquals("api.test", requests[0].url.host)
        assertEquals("10.0.2.2", requests[1].url.host)
        assertEquals(8085, requests[1].url.port)
    }

    @Test
    fun logoutRevokesRefreshTokenAndDropsCachedBearerTokens() = runTest {
        tokenStore.save(SessionTokens("a1", "r1"))
        val profileCache = FakeProfileCache()
        val client = client { respondOk() }
        val repository = AuthRepositoryImpl(AuthApi(client), tokenStore, profileCache)

        client.get(ApiPaths.CURRENT_USER) // Ktor now caches a1 in memory.
        // Real dispatcher: logout's best-effort timeout must not fire on the test's virtual clock.
        withContext(Dispatchers.Default) { repository.logout() }
        client.get(ApiPaths.CURRENT_USER)

        val logout = requests.single { it.url.encodedPath == ApiPaths.LOGOUT }
        assertTrue("\"r1\"" in (logout.body as TextContent).text)
        assertNull(preferences.getRefreshToken())
        assertNull(requests.last().bearer(), "old access token must not be sent after sign-out")
    }
}
