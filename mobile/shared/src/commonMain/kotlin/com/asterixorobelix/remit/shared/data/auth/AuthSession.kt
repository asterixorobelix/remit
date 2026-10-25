package com.asterixorobelix.remit.shared.data.auth

import com.asterixorobelix.remit.shared.data.FirebaseNotSetUpException
import com.asterixorobelix.remit.shared.data.FirebaseSettings

/**
 * The signed-in Firebase user. The app signs in anonymously, so every install has a stable user
 * id: the backend stamps it on each payment, and Firestore rules show users only their own.
 */
interface AuthSession {
    /** Signs in anonymously first if nobody is signed in. */
    suspend fun userId(): String

    /** A Firebase ID token for the API's `Authorization: Bearer` header. */
    suspend fun idToken(forceRefresh: Boolean): String
}

class AuthUnavailableException(val isNetworkError: Boolean, cause: Throwable? = null) :
    Exception("Could not sign in to Firebase", cause) {
    /** Sign-in can never work in this build, so the user should not be told to check their connection. */
    val isNotSetUp: Boolean get() = cause is FirebaseNotSetUpException
}

/** Firebase Auth on Android; an [AuthSession] that always fails where Firebase is not wired. */
expect fun platformAuthSession(settings: FirebaseSettings): AuthSession

/** Where Firebase is not wired (iOS and desktop) or not started (Android without its config). */
internal object UnsupportedPlatformAuthSession : AuthSession {
    override suspend fun userId(): String = throw unsupported()

    override suspend fun idToken(forceRefresh: Boolean): String = throw unsupported()

    private fun unsupported() = AuthUnavailableException(
        isNetworkError = false,
        cause = FirebaseNotSetUpException(),
    )
}
