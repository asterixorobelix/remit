package com.asterixorobelix.remit.domain.auth

/** Result of client-side validation; mirrors the backend's rules so users get instant feedback. */
enum class CredentialsError {
    MISSING_FIELDS,
    INVALID_EMAIL,
    PASSWORD_TOO_SHORT,
    PASSWORDS_DO_NOT_MATCH,
}

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
internal const val MIN_PASSWORD_LENGTH = 8

/** Returns the first problem with the sign-in form, or null when it can be submitted. */
class ValidateLoginUseCase {
    operator fun invoke(email: String, password: String): CredentialsError? = when {
        email.isBlank() || password.isBlank() -> CredentialsError.MISSING_FIELDS
        !EMAIL_PATTERN.matches(email.trim()) -> CredentialsError.INVALID_EMAIL
        else -> null
    }
}

/** Returns the first problem with the registration form, or null when it can be submitted. */
class ValidateRegistrationUseCase {
    operator fun invoke(
        displayName: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): CredentialsError? = when {
        displayName.isBlank() || email.isBlank() || password.isBlank() -> CredentialsError.MISSING_FIELDS
        !EMAIL_PATTERN.matches(email.trim()) -> CredentialsError.INVALID_EMAIL
        password.length < MIN_PASSWORD_LENGTH -> CredentialsError.PASSWORD_TOO_SHORT
        password != confirmPassword -> CredentialsError.PASSWORDS_DO_NOT_MATCH
        else -> null
    }
}
