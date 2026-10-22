package com.asterixorobelix.remit.configuration

/**
 * Firebase for payments. [projectId] comes from `FIREBASE_PROJECT_ID`; credentials come from
 * Application Default Credentials (`GOOGLE_APPLICATION_CREDENTIALS` pointing at a service account
 * key). With `FIRESTORE_EMULATOR_HOST` and `FIREBASE_AUTH_EMULATOR_HOST` set, the Admin SDK talks
 * to the local Firebase Emulator Suite instead and needs no credentials.
 *
 * Fails closed: production requires a project and refuses emulators. Elsewhere, without a project,
 * the server starts and `POST /api/v1/payments` answers 503.
 */
data class FirebaseConfig(
    val projectId: String? = null,
    val firestoreEmulatorHost: String? = null,
    val authEmulatorHost: String? = null,
) {
    val usesEmulators: Boolean get() = firestoreEmulatorHost != null || authEmulatorHost != null

    companion object {
        fun fromEnv(env: EnvLookup, environment: Environment): FirebaseConfig {
            val config = FirebaseConfig(
                projectId = env("FIREBASE_PROJECT_ID")?.trim()?.takeIf(String::isNotEmpty),
                firestoreEmulatorHost = env("FIRESTORE_EMULATOR_HOST")?.trim()?.takeIf(String::isNotEmpty),
                authEmulatorHost = env("FIREBASE_AUTH_EMULATOR_HOST")?.trim()?.takeIf(String::isNotEmpty),
            )
            if (environment.isProduction) {
                require(config.projectId != null) { "FIREBASE_PROJECT_ID is required in production" }
                require(!config.usesEmulators) { "Firebase emulator hosts must not be set in production" }
            }
            return config
        }
    }
}
