package com.example.myproject.domain.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CredentialValidationTest {
    private val validateLogin = ValidateLoginUseCase()
    private val validateRegistration = ValidateRegistrationUseCase()

    @Test
    fun loginRequiresBothFields() {
        assertEquals(CredentialsError.MISSING_FIELDS, validateLogin("", "secret123"))
        assertEquals(CredentialsError.MISSING_FIELDS, validateLogin("a@b.co", " "))
    }

    @Test
    fun loginRejectsMalformedEmail() {
        assertEquals(CredentialsError.INVALID_EMAIL, validateLogin("not-an-email", "secret123"))
    }

    @Test
    fun loginAcceptsValidInput() {
        assertNull(validateLogin(" user@example.com ", "x"))
    }

    @Test
    fun registrationChecksEveryRuleInOrder() {
        assertEquals(CredentialsError.MISSING_FIELDS, validateRegistration("", "a@b.co", "password1", "password1"))
        assertEquals(CredentialsError.INVALID_EMAIL, validateRegistration("Ada", "ada", "password1", "password1"))
        assertEquals(CredentialsError.PASSWORD_TOO_SHORT, validateRegistration("Ada", "a@b.co", "short", "short"))
        assertEquals(
            CredentialsError.PASSWORDS_DO_NOT_MATCH,
            validateRegistration("Ada", "a@b.co", "password1", "password2"),
        )
        assertNull(validateRegistration("Ada", "a@b.co", "password1", "password1"))
    }
}
