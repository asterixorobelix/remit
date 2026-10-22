package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.network.allowRetry
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryViewModel
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentViewModel
import com.asterixorobelix.remit.shared.data.FirebaseSettings
import com.asterixorobelix.remit.shared.data.remote.ApiEndpoint
import com.asterixorobelix.remit.shared.data.remote.PaymentRequestConfigurer
import com.asterixorobelix.remit.shared.di.paymentsModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * The payments feature: the shared module's use cases, repository, and Firebase pieces, plus what
 * this app supplies (API base URL, retry opt-in) and the two screens' ViewModels.
 */
internal fun paymentsModules(config: AppConfig): List<Module> = listOf(
    paymentsModule(FirebaseSettings(emulatorHost = config.firebaseEmulatorHost)),
    module {
        single { ApiEndpoint { get<AppConfig>().resolveApiBaseUrl(get()) } }
        // Retrying the POST is safe: every attempt carries the same Idempotency-Key.
        single { PaymentRequestConfigurer { request -> request.allowRetry() } }
        viewModelOf(::SendPaymentViewModel)
        viewModelOf(::PaymentHistoryViewModel)
    },
)

/** Debug builds honour the Developer-screen override; release builds never do. */
internal fun AppConfig.resolveApiBaseUrl(preferences: AppPreferences): String =
    preferences.getApiBaseUrlOverride()?.takeIf { isDebug } ?: apiBaseUrl
