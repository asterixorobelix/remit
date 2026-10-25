package com.asterixorobelix.remit.shared.data

import com.asterixorobelix.remit.shared.data.auth.AuthSession
import com.asterixorobelix.remit.shared.data.auth.AuthUnavailableException
import com.asterixorobelix.remit.shared.data.history.PaymentHistorySource
import com.asterixorobelix.remit.shared.data.history.PaymentHistoryUnavailableException
import com.asterixorobelix.remit.shared.data.history.toPaymentOrNull
import com.asterixorobelix.remit.shared.data.remote.PaymentsApi
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.HistoryFailure
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.PaymentHistoryException
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Payments go to the API, which records them in Firestore; history comes straight from
 * Firestore. The app never writes payments to Firestore itself, so a payment shows in history only
 * once the server has processed it.
 */
class PaymentRepositoryImpl(
    private val api: PaymentsApi,
    private val history: PaymentHistorySource,
    private val auth: AuthSession,
) : PaymentRepository {
    /** An expired or revoked token gets one retry with a refreshed token, under the same key. */
    override suspend fun send(request: PaymentRequest, idempotencyKey: String): SendPaymentResult {
        val first = sendWithToken(request, idempotencyKey, forceRefresh = false)
        return if (first == UNAUTHORIZED) sendWithToken(request, idempotencyKey, forceRefresh = true) else first
    }

    override fun observeHistory(): Flow<List<Payment>> = flow {
        val payments = history.observe(auth.userId()).map { records ->
            records.mapNotNull { it.toPaymentOrNull() }.sortedByDescending(Payment::createdAt)
        }
        emitAll(payments)
    }.catch { error -> throw PaymentHistoryException(error.historyFailure(), error) }

    private suspend fun sendWithToken(
        request: PaymentRequest,
        idempotencyKey: String,
        forceRefresh: Boolean,
    ): SendPaymentResult = try {
        api.createPayment(request, idempotencyKey, auth.idToken(forceRefresh))
    } catch (expected: AuthUnavailableException) {
        SendPaymentResult.Failed(
            when {
                expected.isNotSetUp -> PaymentFailure.NOT_SET_UP
                expected.isNetworkError -> PaymentFailure.NETWORK
                else -> PaymentFailure.UNAUTHORIZED
            },
        )
    }

    private fun Throwable.historyFailure(): HistoryFailure = when {
        cause is BackendNotSetUpException -> HistoryFailure.NOT_SET_UP
        this is AuthUnavailableException && isNetworkError -> HistoryFailure.NETWORK
        this is PaymentHistoryUnavailableException && isNetworkError -> HistoryFailure.NETWORK
        else -> HistoryFailure.UNAVAILABLE
    }

    private companion object {
        val UNAUTHORIZED = SendPaymentResult.Failed(PaymentFailure.UNAUTHORIZED)
    }
}
