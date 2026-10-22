package com.asterixorobelix.remit.shared.data.history

import com.asterixorobelix.remit.shared.data.FirebaseSettings

/** Firebase is wired on Android only; see the README's cross-platform section. */
actual fun platformPaymentHistorySource(settings: FirebaseSettings): PaymentHistorySource =
    UnsupportedPlatformHistorySource
