package com.asterixorobelix.remit.shared.data.auth

import com.asterixorobelix.remit.shared.data.BackendNotSetUpException

/**
 * The signed-in user. The app signs in anonymously, so every install has a stable user id: the
 * backend stamps it on each payment, and the history store shows users only their own. Firebase
 * Auth implements it on Android (`firebaseBackendModule`).
 */
interface AuthSession {
    /** Signs in anonymously first if nobody is signed in. */
    suspend fun userId(): String

    /** An ID token for the API's `Authorization: Bearer` header. */
    suspend fun idToken(forceRefresh: Boolean): String
}

class AuthUnavailableException(val isNetworkError: Boolean, cause: Throwable? = null) :
    Exception("Could not sign in", cause) {
    /** Sign-in can never work in this build, so the user should not be told to check their connection. */
    val isNotSetUp: Boolean get() = cause is BackendNotSetUpException
}

/** Where no backend is wired (iOS and desktop) or started (Android without its config). */
internal object UnsupportedPlatformAuthSession : AuthSession {
    override suspend fun userId(): String = throw unsupported()

    override suspend fun idToken(forceRefresh: Boolean): String = throw unsupported()

    private fun unsupported() = AuthUnavailableException(
        isNetworkError = false,
        cause = BackendNotSetUpException(),
    )
}
