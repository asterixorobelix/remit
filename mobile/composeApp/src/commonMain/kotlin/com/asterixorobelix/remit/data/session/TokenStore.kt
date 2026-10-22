package com.asterixorobelix.remit.data.session

import com.asterixorobelix.remit.data.preferences.AppPreferences
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Access/refresh token pair persisted in platform secure storage. */
data class SessionTokens(val accessToken: String, val refreshToken: String)

/**
 * Single writer for the persisted session tokens.
 *
 * Refresh tokens rotate on every `/auth/refresh`, so writes that originate from a refresh are
 * compare-and-set against the refresh token that was sent: a refresh that finishes after a
 * sign-out or a new sign-in can never overwrite the newer session.
 */
class TokenStore(private val preferences: AppPreferences) {
    private val mutex = Mutex()

    fun current(): SessionTokens? {
        val access = preferences.getAccessToken() ?: return null
        val refresh = preferences.getRefreshToken() ?: return null
        return SessionTokens(access, refresh)
    }

    fun refreshToken(): String? = preferences.getRefreshToken()

    suspend fun save(tokens: SessionTokens) = mutex.withLock { write(tokens) }

    /** Stores [rotated] only if [sentRefreshToken] is still the current refresh token. */
    suspend fun replaceIfCurrent(sentRefreshToken: String, rotated: SessionTokens): Boolean =
        mutex.withLock {
            if (preferences.getRefreshToken() != sentRefreshToken) return@withLock false
            write(rotated)
            true
        }

    /** Clears the session only if [rejectedRefreshToken] is still current. */
    suspend fun clearIfCurrent(rejectedRefreshToken: String): Boolean = mutex.withLock {
        if (preferences.getRefreshToken() != rejectedRefreshToken) return@withLock false
        preferences.clearAuthTokens()
        true
    }

    suspend fun clear() = mutex.withLock { preferences.clearAuthTokens() }

    private fun write(tokens: SessionTokens) {
        preferences.setAccessToken(tokens.accessToken)
        preferences.setRefreshToken(tokens.refreshToken)
    }
}
