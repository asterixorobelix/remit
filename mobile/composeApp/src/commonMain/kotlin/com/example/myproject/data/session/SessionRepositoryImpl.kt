package com.example.myproject.data.session

import com.example.myproject.data.preferences.AppPreferences
import com.example.myproject.domain.session.ProfileCache
import com.example.myproject.domain.session.SessionRepository
import com.example.myproject.domain.session.UserProfile
import com.example.myproject.platform.AppDispatchers
import com.example.myproject.util.runSuspendCatching
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class SessionRepositoryImpl(
    private val tokenStore: TokenStore,
    private val profileCache: ProfileCache,
    private val events: SessionEvents,
    private val dispatchers: AppDispatchers,
    private val httpClient: HttpClient,
) : SessionRepository {
    // Token reads decrypt from Keystore/Keychain, so keep them off the main thread. A storage
    // failure means "signed out" rather than a crash at launch.
    override suspend fun hasSession(): Boolean =
        runSuspendCatching { withContext(dispatchers.io) { tokenStore.refreshToken() != null } }.getOrDefault(false)

    override val sessionExpired: Flow<Unit> = events.expired

    override suspend fun clearLocalSession() {
        withContext(dispatchers.io) {
            tokenStore.clear()
            profileCache.clear()
        }
        httpClient.authProvider<BearerAuthProvider>()?.clearToken()
    }
}

internal class PreferencesProfileCache(private val preferences: AppPreferences) : ProfileCache {
    override fun cachedProfile(): UserProfile? {
        val email = preferences.getUserEmail() ?: return null
        return UserProfile(displayName = preferences.getUserDisplayName().orEmpty(), email = email)
    }

    override fun save(profile: UserProfile) {
        preferences.setUserDisplayName(profile.displayName)
        preferences.setUserEmail(profile.email)
    }

    override fun clear() {
        preferences.setUserDisplayName(null)
        preferences.setUserEmail(null)
    }
}
