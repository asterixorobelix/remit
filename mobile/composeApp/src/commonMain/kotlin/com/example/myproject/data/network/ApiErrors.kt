package com.example.myproject.data.network

import com.example.myproject.domain.common.DataError
import com.example.myproject.domain.common.DataException
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.io.IOException

/** Non-2xx response from the API. */
class ApiException(val status: HttpStatusCode) : Exception("HTTP ${status.value}")

internal fun HttpResponse.ensureSuccess(): HttpResponse {
    if (!status.isSuccess()) throw ApiException(status)
    return this
}

internal suspend inline fun <reified T> HttpResponse.bodyOrThrow(): T = ensureSuccess().body()

/**
 * Maps transport and HTTP failures to [DataException]. [statusOverrides] lets a call site give
 * endpoint-specific meaning to a status (for example 401 on login = invalid credentials).
 */
internal fun Throwable.toDataException(
    statusOverrides: Map<HttpStatusCode, DataError> = emptyMap(),
): DataException = when (this) {
    is DataException -> this
    is ApiException -> DataException(statusOverrides[status] ?: status.toDataError(), this)
    is IOException -> DataException(DataError.NETWORK, this)
    else -> DataException(DataError.UNKNOWN, this)
}

private fun HttpStatusCode.toDataError(): DataError = when {
    this == HttpStatusCode.Unauthorized || this == HttpStatusCode.Forbidden -> DataError.UNAUTHORIZED
    this == HttpStatusCode.TooManyRequests -> DataError.RATE_LIMITED
    this == HttpStatusCode.BadRequest -> DataError.INVALID_INPUT
    value >= HttpStatusCode.InternalServerError.value -> DataError.SERVER
    else -> DataError.UNKNOWN
}
