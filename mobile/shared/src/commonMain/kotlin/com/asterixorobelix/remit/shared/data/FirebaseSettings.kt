package com.asterixorobelix.remit.shared.data

/**
 * How the app reaches Firebase. With [emulatorHost] set (for example `10.0.2.2` from the Android
 * emulator), Auth and Firestore use the local Firebase Emulator Suite instead of the real project.
 */
data class FirebaseSettings(val emulatorHost: String? = null) {
    companion object {
        const val AUTH_EMULATOR_PORT = 9099
        const val FIRESTORE_EMULATOR_PORT = 8080

        /**
         * The project the debug app uses against the emulators when it has no google-services.json.
         * It must match `FIREBASE_PROJECT` in the root Makefile (`make emulators`,
         * `make backend-emulated`), or the backend writes payments History never sees.
         */
        const val EMULATOR_PROJECT_ID = "remit-payments"
    }
}
