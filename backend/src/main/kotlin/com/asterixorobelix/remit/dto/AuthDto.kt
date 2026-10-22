package com.asterixorobelix.remit.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val displayName: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class RefreshRequest(
    val refreshToken: String,
)

@Serializable
data class LogoutRequest(
    val refreshToken: String,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
)

@Serializable
data class UserResponse(
    val id: Long,
    val email: String,
    val displayName: String,
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
)

/** The single error shape returned by every endpoint. */
@Serializable
data class ErrorResponse(
    val error: String,
)

@Serializable
data class AppConfigResponse(
    val minVersion: String,
    val latestVersion: String,
    val forceUpdate: Boolean,
    val maintenanceMode: Boolean,
)
