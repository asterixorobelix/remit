package com.asterixorobelix.remit.platform

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * [CrashLogger] backed by Firebase Crashlytics: an adapter, the only class that calls Crashlytics.
 * Chosen by `androidObservabilityProviders`. Collection is off in the manifest until [initialize].
 */
class CrashlyticsCrashLogger(private val crashlytics: FirebaseCrashlytics) : CrashLogger {
    override fun initialize() {
        crashlytics.isCrashlyticsCollectionEnabled = true
    }

    override fun logException(throwable: Throwable, message: String?) {
        message?.let(crashlytics::log)
        crashlytics.recordException(throwable)
    }

    override fun setCustomKey(key: String, value: String) = crashlytics.setCustomKey(key, value)

    override fun setUserId(userId: String) = crashlytics.setUserId(userId)

    override fun log(message: String) = crashlytics.log(message)

    override fun testCrash() {
        error("Test crash for Firebase Crashlytics")
    }
}
