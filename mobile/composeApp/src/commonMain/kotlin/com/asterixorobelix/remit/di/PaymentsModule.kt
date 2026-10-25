package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.network.RetryPolicy
import com.asterixorobelix.remit.data.network.allowRetry
import com.asterixorobelix.remit.data.payments.SqlDelightPendingPaymentStore
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryViewModel
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentViewModel
import com.asterixorobelix.remit.shared.data.FirebaseSettings
import com.asterixorobelix.remit.shared.data.firebaseBackendModule
import com.asterixorobelix.remit.shared.data.remote.PaymentRequestConfigurer
import com.asterixorobelix.remit.shared.di.paymentsModule
import com.asterixorobelix.remit.shared.domain.repository.PendingPaymentStore
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The payments feature: the shared module's use cases and repository, the backend, plus what
 * this app supplies (durable pending payment, retry opt-in) and the screens' ViewModels. The API
 * base URL (`ApiEndpoint`) comes from the network module.
 */
internal fun paymentsModules(config: AppConfig): List<Module> = listOf(
    paymentsModule(),
    // The backend (sign-in and History). To replace Firebase, bind another AuthSession and
    // PaymentHistorySource here; nothing else names the provider.
    firebaseBackendModule(config.firebaseSettings()),
    module {
        single<PendingPaymentStore> { SqlDelightPendingPaymentStore(get(), get()) }
        single { PaymentRequestConfigurer { request -> request.paymentRequestOptions() } }
        viewModelOf(::SendPaymentViewModel)
        viewModelOf(::PaymentHistoryViewModel)
    },
)

/**
 * Retrying the POST is safe: every attempt carries the same Idempotency-Key. One quick retry and a
 * shorter timeout than the client's (3 retries, 30 s) keep a hung or stopped backend from leaving
 * the Send button spinning for minutes; after that the user sees the error and can Retry.
 */
internal fun HttpRequestBuilder.paymentRequestOptions() {
    allowRetry(PAYMENT_RETRY_POLICY)
    timeout { requestTimeoutMillis = PAYMENT_REQUEST_TIMEOUT_MS }
}

internal val PAYMENT_RETRY_POLICY = RetryPolicy(maxRetries = 1, initialDelayMs = 500, maxDelayMs = 2_000)
internal const val PAYMENT_REQUEST_TIMEOUT_MS = 10_000L

internal fun AppConfig.firebaseSettings() = FirebaseSettings(emulatorHost = firebaseEmulatorHost)
