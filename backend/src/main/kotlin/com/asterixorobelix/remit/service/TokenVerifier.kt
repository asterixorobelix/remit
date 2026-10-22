package com.asterixorobelix.remit.service

import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Resolves a Firebase ID token to the caller's user id, or null when it is not valid. */
fun interface TokenVerifier {
    /** @throws TokenVerificationUnavailableException when the token could not be checked at all. */
    suspend fun userId(idToken: String): String?
}

/**
 * The token could not be checked, for example because Google's signing keys could not be fetched.
 * That says nothing about the token, so it is a 503 the app retries, not a 401.
 */
class TokenVerificationUnavailableException(cause: Throwable) : Exception("Could not verify the ID token", cause)

/**
 * Verifies signature, expiry, issuer, and audience (the Firebase project). Revocation is not
 * checked: it costs a network call per request, and ID tokens expire within an hour anyway.
 *
 * Only failures that are about the token itself are a 401 (null); any other failure is
 * [TokenVerificationUnavailableException], so a new failure mode is never mistaken for a bad token.
 */
class FirebaseTokenVerifier internal constructor(private val verify: (String) -> String) : TokenVerifier {
    constructor(auth: FirebaseAuth) : this({ token -> auth.verifyIdToken(token).uid })

    override suspend fun userId(idToken: String): String? = withContext(Dispatchers.IO) {
        try {
            verify(idToken)
        } catch (error: FirebaseAuthException) {
            if (error.authErrorCode in TOKEN_REJECTED) null else throw TokenVerificationUnavailableException(error)
        } catch (expected: IllegalArgumentException) {
            null
        }
    }

    private companion object {
        val TOKEN_REJECTED = setOf(
            AuthErrorCode.INVALID_ID_TOKEN,
            AuthErrorCode.EXPIRED_ID_TOKEN,
            AuthErrorCode.REVOKED_ID_TOKEN,
            AuthErrorCode.USER_DISABLED,
            AuthErrorCode.USER_NOT_FOUND,
            AuthErrorCode.TENANT_ID_MISMATCH,
        )
    }
}
