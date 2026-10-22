package com.asterixorobelix.remit.data.network

import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.domain.common.DataException
import com.asterixorobelix.remit.domain.common.toDataError
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ApiErrorsTest {
    private fun HttpStatusCode.mapped(overrides: Map<HttpStatusCode, DataError> = emptyMap()) =
        ApiException(this).toDataException(overrides).error

    @Test
    fun httpStatusesMapToTypedErrors() {
        assertEquals(DataError.UNAUTHORIZED, HttpStatusCode.Unauthorized.mapped())
        assertEquals(DataError.UNAUTHORIZED, HttpStatusCode.Forbidden.mapped())
        assertEquals(DataError.RATE_LIMITED, HttpStatusCode.TooManyRequests.mapped())
        assertEquals(DataError.INVALID_INPUT, HttpStatusCode.BadRequest.mapped())
        assertEquals(DataError.SERVER, HttpStatusCode.InternalServerError.mapped())
        assertEquals(DataError.SERVER, HttpStatusCode.ServiceUnavailable.mapped())
        assertEquals(DataError.UNKNOWN, HttpStatusCode.NotFound.mapped())
    }

    @Test
    fun callSitesCanOverrideAStatus() {
        val overrides = mapOf(HttpStatusCode.Conflict to DataError.INVALID_INPUT)
        assertEquals(DataError.INVALID_INPUT, HttpStatusCode.Conflict.mapped(overrides))
    }

    @Test
    fun transportAndUnexpectedFailuresAreClassified() {
        assertEquals(DataError.NETWORK, IOException("offline").toDataException().error)
        assertEquals(DataError.UNKNOWN, IllegalStateException("bug").toDataException().error)
        val existing = DataException(DataError.SERVER)
        assertSame(existing, existing.toDataException())
    }

    @Test
    fun toDataErrorFallsBackToUnknown() {
        assertEquals(DataError.RATE_LIMITED, DataException(DataError.RATE_LIMITED).toDataError())
        assertEquals(DataError.UNKNOWN, IllegalArgumentException().toDataError())
    }

    @Test
    fun nonSuccessResponsesThrowApiException() = runTest {
        val client = HttpClient(MockEngine { respond("", HttpStatusCode.BadGateway) })
        val error = assertFailsWith<ApiException> { client.get("https://api.test/x").ensureSuccess() }
        assertEquals(HttpStatusCode.BadGateway, error.status)
    }
}
