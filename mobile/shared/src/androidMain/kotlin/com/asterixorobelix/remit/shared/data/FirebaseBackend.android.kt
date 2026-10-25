package com.asterixorobelix.remit.shared.data

import android.content.Context
import com.asterixorobelix.remit.shared.data.auth.AuthSession
import com.asterixorobelix.remit.shared.data.auth.firebaseAuthSession
import com.asterixorobelix.remit.shared.data.history.PaymentHistorySource
import com.asterixorobelix.remit.shared.data.history.firestorePaymentHistorySource
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun firebaseBackendModule(settings: FirebaseSettings): Module = module {
    single<AuthSession> { firebaseAuthSession(settings) }
    single<PaymentHistorySource> { firestorePaymentHistorySource(settings) }
}

/**
 * Starts Firebase for the local Emulator Suite when the build has no google-services.json (a fresh
 * clone), so the debug app runs without a Firebase project or credentials. Call it before the
 * payments feature is used. It does nothing when Firebase is already started (the Google Services
 * plugin did it from google-services.json) or when no emulator host is set.
 */
fun startFirebaseForEmulators(context: Context, settings: FirebaseSettings) {
    if (settings.emulatorHost == null || FirebaseApp.getApps(context).isNotEmpty()) return
    val options = FirebaseOptions.Builder()
        .setProjectId(FirebaseSettings.EMULATOR_PROJECT_ID)
        .setApplicationId(EMULATOR_APP_ID)
        .setApiKey(EMULATOR_API_KEY)
        .build()
    FirebaseApp.initializeApp(context, options)
}

/** The default app, or null when Firebase was never started (no google-services.json, no emulator). */
internal fun defaultFirebaseApp(): FirebaseApp? = try {
    FirebaseApp.getInstance()
} catch (expected: IllegalStateException) {
    null
}

// The emulators accept any app id and API key; these only have to be well formed.
private const val EMULATOR_APP_ID = "1:000000000000:android:0000000000000000"
private const val EMULATOR_API_KEY = "emulator-api-key"
