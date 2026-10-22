package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.configuration.FirebaseConfig
import com.asterixorobelix.remit.routes.PaymentsFeature
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.cloud.FirestoreClient

/**
 * Connects the payments feature to Firebase, or returns null when no project is configured. The
 * Admin SDK reads the emulator hosts from the environment itself; they only change credentials here.
 */
object FirebaseBackend {
    private const val APP_NAME = "remit-backend"

    fun paymentsFeature(config: FirebaseConfig): PaymentsFeature? = config.projectId?.let { projectId ->
        val app = FirebaseApp.getApps().firstOrNull { it.name == APP_NAME } ?: FirebaseApp.initializeApp(
            FirebaseOptions.builder()
                .setProjectId(projectId)
                .setCredentials(
                    if (config.usesEmulators) emulatorCredentials() else GoogleCredentials.getApplicationDefault(),
                )
                .build(),
            APP_NAME,
        )
        PaymentsFeature(
            service = PaymentService(FirestorePaymentStore(FirestoreClient.getFirestore(app))),
            tokens = FirebaseTokenVerifier(FirebaseAuth.getInstance(app)),
        )
    }

    /** The emulators accept any token; this avoids looking for real credentials. */
    private fun emulatorCredentials(): GoogleCredentials = GoogleCredentials.create(AccessToken("owner", null))
}
