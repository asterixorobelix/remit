package com.asterixorobelix.remit.shared.data

/**
 * This build has no payments backend configured: iOS and desktop do not wire Firebase, and an
 * Android build needs `google-services.json` or, for debug builds, the emulators (the default
 * without it, unless `FIREBASE_EMULATOR_HOST` is set blank).
 * Retrying cannot help, so screens say so instead of blaming the connection.
 */
class BackendNotSetUpException : Exception("The payments backend is not set up in this build")
