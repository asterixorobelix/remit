package com.example.myproject.data.auth

import com.example.myproject.data.network.ApiException
import com.example.myproject.data.network.toDataException
import com.example.myproject.data.session.SessionTokens
import com.example.myproject.data.session.TokenStore
import com.example.myproject.domain.auth.AuthRepository
import com.example.myproject.domain.auth.AuthUser
import com.example.myproject.domain.common.DataError
import com.example.myproject.domain.session.ProfileCache
import com.example.myproject.domain.session.UserProfile
import com.example.myproject.util.runSuspendCatching
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.withTimeoutOrNull

private const val LOGOUT_TIMEOUT_MS = 5_000L

private val LOGIN_ERRORS = mapOf(HttpStatusCode.Unauthorized to DataError.INVALID_CREDENTIALS)
private val REGISTER_ERRORS = mapOf(HttpStatusCode.Conflict to DataError.EMAIL_TAKEN)

internal class AuthRepositoryImpl(
    private val api: AuthApi,
    private val tokenStore: TokenStore,
    private val profileCache: ProfileCache,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthUser> =
        authenticate(LOGIN_ERRORS) { api.login(email, password) }

    override suspend fun register(displayName: String, email: String, password: String): Result<AuthUser> =
        authenticate(REGISTER_ERRORS) { api.register(displayName, email, password) }

    override suspend fun logout() {
        val refreshToken = tokenStore.refreshToken()
        if (refreshToken != null) {
            // Best effort: an offline sign-out must still succeed locally.
            withTimeoutOrNull(LOGOUT_TIMEOUT_MS) { runSuspendCatching { api.logout(refreshToken) } }
        }
        clearLocalSession()
    }

    override suspend fun deleteAccount(): Result<Unit> =
        runSuspendCatching { api.deleteAccount() }
            // 404: the account is already gone, which is the outcome the user asked for.
            .recoverCatching { error -> if (!error.isNotFound()) throw error.toDataException() }
            .map { clearLocalSession() }

    override suspend fun refreshProfile(): Result<UserProfile> =
        runSuspendCatching { api.currentUser().toProfile().also(profileCache::save) }
            .recoverCatching { throw it.toDataException() }

    override suspend fun updateDisplayName(displayName: String): Result<UserProfile> =
        runSuspendCatching { api.updateDisplayName(displayName).toProfile().also(profileCache::save) }
            .recoverCatching { throw it.toDataException() }

    private suspend fun authenticate(
        statusOverrides: Map<HttpStatusCode, DataError>,
        call: suspend () -> AuthResponse,
    ): Result<AuthUser> = runSuspendCatching {
        val response = call()
        // Drop any tokens Ktor cached for a previous session before storing the new one.
        api.clearCachedTokens()
        tokenStore.save(SessionTokens(response.accessToken, response.refreshToken))
        profileCache.save(response.user.toProfile())
        AuthUser(response.user.id, response.user.email, response.user.displayName)
    }.recoverCatching { throw it.toDataException(statusOverrides) }

    private suspend fun clearLocalSession() {
        tokenStore.clear()
        profileCache.clear()
        api.clearCachedTokens()
    }
}

private fun UserResponse.toProfile() = UserProfile(displayName = displayName, email = email)

private fun Throwable.isNotFound() = this is ApiException && status == HttpStatusCode.NotFound
