package com.asterixorobelix.remit.shared.data.auth

import com.asterixorobelix.remit.shared.data.FirebaseSettings

/** Firebase is wired on Android only; see the README's cross-platform section. */
actual fun platformAuthSession(settings: FirebaseSettings): AuthSession = UnsupportedPlatformAuthSession
