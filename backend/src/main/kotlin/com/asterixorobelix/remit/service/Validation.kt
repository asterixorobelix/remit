package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.DISPLAY_NAME_MAX_LENGTH
import com.asterixorobelix.remit.domain.EMAIL_MAX_LENGTH

/**
 * A deliberate client input error. Its message is safe to return to clients (HTTP 400);
 * every other exception is reported as a generic 500.
 */
class ValidationException(message: String) : RuntimeException(message)

internal fun validationError(message: String): Nothing = throw ValidationException(message)

internal object InputRules {
    const val MIN_PASSWORD_CHARS = 8

    /** BCrypt only uses the first 72 bytes of a password. */
    const val MAX_PASSWORD_BYTES = 72

    private val emailPattern = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun email(raw: String): String {
        val email = normalizeEmail(raw)
        if (email.length > EMAIL_MAX_LENGTH) validationError("Email must be at most $EMAIL_MAX_LENGTH characters")
        if (!emailPattern.matches(email)) validationError("Invalid email address")
        return email
    }

    fun password(raw: String) {
        if (raw.length < MIN_PASSWORD_CHARS) {
            validationError("Password must be at least $MIN_PASSWORD_CHARS characters")
        }
        if (raw.toByteArray(Charsets.UTF_8).size > MAX_PASSWORD_BYTES) {
            validationError("Password must be at most $MAX_PASSWORD_BYTES bytes")
        }
    }

    fun displayName(raw: String): String {
        val name = raw.trim()
        if (name.isEmpty()) validationError("Display name must not be blank")
        if (name.length > DISPLAY_NAME_MAX_LENGTH) {
            validationError("Display name must be at most $DISPLAY_NAME_MAX_LENGTH characters")
        }
        return name
    }
}
