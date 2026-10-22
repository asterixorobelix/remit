package com.asterixorobelix.remit.service

import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolves a Firebase ID token to the caller's user id, or null when it is not valid. */
fun interface TokenVerifier {
    suspend fun userId(idToken: String): String?
}

/**
 * Verifies signature, expiry, issuer, and audience (the Firebase project). Revocation is not
 * checked: it costs a network call per request, and ID tokens expire within an hour anyway.
 *
 * A token that fails verification is a 401. Failing to fetch Google's signing keys says nothing
 * about the token, so it is rethrown and becomes a 500, which the app retries.
 */
class FirebaseTokenVerifier(private val auth: FirebaseAuth) : TokenVerifier {
    override suspend fun userId(idToken: String): String? = withContext(Dispatchers.IO) {
        try {
            auth.verifyIdToken(idToken).uid
        } catch (error: FirebaseAuthException) {
            if (error.authErrorCode == AuthErrorCode.CERTIFICATE_FETCH_FAILED) throw error
            null
        } catch (expected: IllegalArgumentException) {
            null
        }
    }
}
