package com.example.myproject.domain.session

import kotlinx.coroutines.flow.Flow

/** Locally cached profile of the signed-in user (display name and email only). */
data class UserProfile(
    val displayName: String,
    val email: String,
)

/** Cache of the signed-in user's profile so screens render without a network round trip. */
interface ProfileCache {
    fun cachedProfile(): UserProfile?
    fun save(profile: UserProfile)
    fun clear()
}

/** Session state shared across features. */
interface SessionRepository {
    /** True when a refresh token is stored, i.e. the user is signed in on this device. */
    suspend fun hasSession(): Boolean

    /** Emits when the server rejected the refresh token and the local session was cleared. */
    val sessionExpired: Flow<Unit>

    /** Removes tokens, the cached profile and in-memory bearer tokens without calling the server. */
    suspend fun clearLocalSession()
}

/**
 * Implemented by features that keep per-user data on the device (for example the Tasks
 * database). Sign-out and account deletion call every registered cleaner so the next
 * account never sees or syncs the previous user's data.
 */
fun interface UserDataCleaner {
    suspend fun clearUserData()
}
