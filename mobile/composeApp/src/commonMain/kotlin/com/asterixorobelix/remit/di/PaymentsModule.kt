package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.network.allowRetry
import com.asterixorobelix.remit.data.payments.SqlDelightPendingPaymentStore
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryViewModel
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentViewModel
import com.asterixorobelix.remit.shared.data.FirebaseSettings
import com.asterixorobelix.remit.shared.data.remote.ApiEndpoint
import com.asterixorobelix.remit.shared.data.remote.PaymentRequestConfigurer
import com.asterixorobelix.remit.shared.di.paymentsModule
import com.asterixorobelix.remit.shared.domain.repository.PendingPaymentStore
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The payments feature: the shared module's use cases, repository, and Firebase pieces, plus what
 * this app supplies (API base URL, durable pending payment, retry opt-in) and the screens' ViewModels.
 */
internal fun paymentsModules(config: AppConfig): List<Module> = listOf(
    paymentsModule(config.firebaseSettings()),
    module {
        single { ApiEndpoint { get<AppConfig>().resolveApiBaseUrl(get()) } }
        single<PendingPaymentStore> { SqlDelightPendingPaymentStore(get(), get()) }
        // Retrying the POST is safe: every attempt carries the same Idempotency-Key.
        single { PaymentRequestConfigurer { request -> request.allowRetry() } }
        viewModelOf(::SendPaymentViewModel)
        viewModelOf(::PaymentHistoryViewModel)
    },
)

internal fun AppConfig.firebaseSettings() = FirebaseSettings(emulatorHost = firebaseEmulatorHost)

/** Debug builds honour the Developer-screen override; release builds never do. */
internal fun AppConfig.resolveApiBaseUrl(preferences: AppPreferences): String =
    preferences.getApiBaseUrlOverride()?.takeIf { isDebug } ?: apiBaseUrl
