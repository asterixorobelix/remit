package com.asterixorobelix.remit.data.auth

import kotlinx.serialization.Serializable

@Serializable internal data class LoginRequest(val email: String, val password: String)

@Serializable internal data class RegisterRequest(val email: String, val password: String, val displayName: String)

@Serializable internal data class RefreshRequest(val refreshToken: String)

@Serializable internal data class LogoutRequest(val refreshToken: String)

@Serializable internal data class UpdateProfileRequest(val displayName: String)

@Serializable internal data class UserResponse(val id: Long, val email: String, val displayName: String)

@Serializable internal data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
)
