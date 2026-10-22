package com.asterixorobelix.remit.data.auth

import com.asterixorobelix.remit.data.network.ApiPaths
import com.asterixorobelix.remit.data.network.bodyOrThrow
import com.asterixorobelix.remit.data.network.ensureSuccess
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody

internal class AuthApi(private val client: HttpClient) {
    suspend fun login(email: String, password: String): AuthResponse =
        client.post(ApiPaths.LOGIN) { setBody(LoginRequest(email, password)) }.bodyOrThrow()

    suspend fun register(displayName: String, email: String, password: String): AuthResponse =
        client.post(ApiPaths.REGISTER) {
            setBody(RegisterRequest(email, password, displayName))
        }.bodyOrThrow()

    /** Revokes [refreshToken] on the server (204). */
    suspend fun logout(refreshToken: String) {
        client.post(ApiPaths.LOGOUT) { setBody(LogoutRequest(refreshToken)) }.ensureSuccess()
    }

    suspend fun currentUser(): UserResponse = client.get(ApiPaths.CURRENT_USER).bodyOrThrow()

    suspend fun updateDisplayName(displayName: String): UserResponse =
        client.put(ApiPaths.CURRENT_USER) { setBody(UpdateProfileRequest(displayName)) }.bodyOrThrow()

    /** Permanently deletes the signed-in account (204). */
    suspend fun deleteAccount() {
        client.delete(ApiPaths.CURRENT_USER).ensureSuccess()
    }

    /** Drops the bearer tokens Ktor cached in memory so the next request reloads them. */
    fun clearCachedTokens() {
        client.authProvider<BearerAuthProvider>()?.clearToken()
    }
}
