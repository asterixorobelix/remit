package com.asterixorobelix.remit.service

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
 */
class FirebaseTokenVerifier(private val auth: FirebaseAuth) : TokenVerifier {
    override suspend fun userId(idToken: String): String? = withContext(Dispatchers.IO) {
        try {
            auth.verifyIdToken(idToken).uid
        } catch (expected: FirebaseAuthException) {
            null
        } catch (expected: IllegalArgumentException) {
            null
        }
    }
}
