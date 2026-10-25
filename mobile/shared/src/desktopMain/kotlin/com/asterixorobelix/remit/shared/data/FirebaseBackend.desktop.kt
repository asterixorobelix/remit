package com.asterixorobelix.remit.shared.data

import com.asterixorobelix.remit.shared.data.auth.AuthSession
import com.asterixorobelix.remit.shared.data.auth.UnsupportedPlatformAuthSession
import com.asterixorobelix.remit.shared.data.history.PaymentHistorySource
import com.asterixorobelix.remit.shared.data.history.UnsupportedPlatformHistorySource
import org.koin.core.module.Module
import org.koin.dsl.module

/** Firebase is wired on Android only; see the README's cross-platform section. */
@Suppress("UnusedParameter") // Same signature on every platform; only Android reads the settings.
actual fun firebaseBackendModule(settings: FirebaseSettings): Module = module {
    single<AuthSession> { UnsupportedPlatformAuthSession }
    single<PaymentHistorySource> { UnsupportedPlatformHistorySource }
}
