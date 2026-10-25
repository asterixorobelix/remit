package com.asterixorobelix.remit.platform

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Android crash reporting. When [firebaseEnabled] (the `firebase` capability, not the emulators)
 * and Firebase was started from `google-services.json`, reports go to Crashlytics; otherwise they
 * are written to logcat. Collection is off in the manifest until [initialize] turns it on, so an
 * emulator or unconfigured build never reports.
 */
class AndroidCrashLogger(private val firebaseEnabled: Boolean) : CrashLogger {
    private val tag = "CrashLogger"
    private var crashlytics: FirebaseCrashlytics? = null

    override fun initialize() {
        if (!firebaseEnabled) {
            Log.i(tag, "Firebase crash reporting disabled - logging errors locally")
            return
        }
        crashlytics = try {
            FirebaseCrashlytics.getInstance().apply { isCrashlyticsCollectionEnabled = true }
        } catch (notStarted: IllegalStateException) {
            Log.w(tag, "Crashlytics unavailable (is google-services.json configured?)", notStarted)
            null
        }
    }

    override fun logException(throwable: Throwable, message: String?) {
        val crashlytics = crashlytics
        if (crashlytics == null) {
            Log.e(tag, message ?: "Exception occurred", throwable)
            return
        }
        message?.let(crashlytics::log)
        crashlytics.recordException(throwable)
    }

    override fun setCustomKey(key: String, value: String) {
        crashlytics?.setCustomKey(key, value) ?: Log.d(tag, "Custom key: $key = $value")
    }

    override fun setUserId(userId: String) {
        crashlytics?.setUserId(userId)
    }

    override fun log(message: String) {
        crashlytics?.log(message) ?: Log.d(tag, message)
    }

    override fun testCrash() {
        error("Test crash for Firebase Crashlytics")
    }
}

actual fun createCrashLogger(firebaseEnabled: Boolean): CrashLogger = AndroidCrashLogger(firebaseEnabled)
