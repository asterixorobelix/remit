package com.asterixorobelix.remit.platform

import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * The Android crash-reporting and analytics providers, and the only code that chooses them:
 * Firebase (Crashlytics and Analytics) when this build reports to a real Firebase project, logcat
 * otherwise. To use another provider, say Sentry, add an adapter that implements [CrashLogger]
 * (like [CrashlyticsCrashLogger]), return it here, and swap the Gradle dependency. Nothing else
 * changes: shared code depends only on the interfaces, and redaction wraps whatever is returned.
 */
fun androidObservabilityProviders(config: AppConfig): ObservabilityProviders {
    val firebase = if (config.reportsToFirebase()) firebaseAppOrNull() else null
    return if (firebase == null) {
        ObservabilityProviders(LogcatCrashLogger(), LogcatAnalytics())
    } else {
        ObservabilityProviders(
            crashLogger = CrashlyticsCrashLogger(FirebaseCrashlytics.getInstance()),
            analytics = FirebaseAnalyticsTracker(FirebaseAnalytics.getInstance(firebase.applicationContext)),
        )
    }
}

/** A real Firebase project: the `firebase` capability, and not the local emulators. */
private fun AppConfig.reportsToFirebase(): Boolean = has(Capability.FIREBASE) && firebaseEmulatorHost == null

/** The app Firebase started from google-services.json, or null when the build has none. */
private fun firebaseAppOrNull(): FirebaseApp? = try {
    FirebaseApp.getInstance()
} catch (notStarted: IllegalStateException) {
    Log.w("Observability", "Firebase is not configured (no google-services.json)", notStarted)
    null
}
