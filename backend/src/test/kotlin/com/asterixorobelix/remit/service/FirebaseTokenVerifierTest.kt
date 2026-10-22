package com.asterixorobelix.remit.service

import com.google.firebase.ErrorCode
import com.google.firebase.auth.AuthErrorCode
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FirebaseTokenVerifierTest {
    private fun failingWith(code: AuthErrorCode?) = FirebaseTokenVerifier {
        throw FirebaseAuthException(ErrorCode.INVALID_ARGUMENT, "rejected", null, null, code)
    }

    @Test
    fun aValidTokenResolvesToItsUser() = runBlocking {
        assertEquals("uid-1", FirebaseTokenVerifier { "uid-1" }.userId("token"))
    }

    @Test
    fun aBadTokenIsNoUser() = runBlocking {
        listOf(
            AuthErrorCode.INVALID_ID_TOKEN,
            AuthErrorCode.EXPIRED_ID_TOKEN,
            AuthErrorCode.REVOKED_ID_TOKEN,
            AuthErrorCode.USER_DISABLED,
            AuthErrorCode.USER_NOT_FOUND,
            AuthErrorCode.TENANT_ID_MISMATCH,
        ).forEach { code -> assertNull(failingWith(code).userId("token"), code.name) }
        assertNull(FirebaseTokenVerifier { throw IllegalArgumentException("empty") }.userId(""))
    }

    @Test
    fun failingToFetchSigningKeysIsUnavailableNotABadToken() {
        assertFailsWith<TokenVerificationUnavailableException> {
            runBlocking { failingWith(AuthErrorCode.CERTIFICATE_FETCH_FAILED).userId("token") }
        }
    }

    @Test
    fun anyOtherAuthFailureIsUnavailable() {
        listOf(AuthErrorCode.CONFIGURATION_NOT_FOUND, null).forEach { code ->
            assertFailsWith<TokenVerificationUnavailableException>(code?.name) {
                runBlocking { failingWith(code).userId("token") }
            }
        }
    }
}
