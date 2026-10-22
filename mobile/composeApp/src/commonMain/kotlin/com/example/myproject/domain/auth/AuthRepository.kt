package com.example.myproject.domain.auth

import com.example.myproject.domain.session.UserProfile

data class AuthUser(
    val id: Long,
    val email: String,
    val displayName: String,
)

/**
 * Account and session operations. Failures are reported as
 * [com.example.myproject.domain.common.DataException].
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthUser>
    suspend fun register(displayName: String, email: String, password: String): Result<AuthUser>

    /** Best-effort server revocation, then always clears the local session. */
    suspend fun logout()

    /** Deletes the account on the server and, on success, clears the local session. */
    suspend fun deleteAccount(): Result<Unit>

    /** Fetches the profile from the server and refreshes the local cache. */
    suspend fun refreshProfile(): Result<UserProfile>

    suspend fun updateDisplayName(displayName: String): Result<UserProfile>
}
