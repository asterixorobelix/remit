package com.example.myproject.domain.auth

import com.example.myproject.domain.common.DataError
import com.example.myproject.domain.common.DataException
import com.example.myproject.domain.session.UserDataCleaner
import com.example.myproject.domain.session.UserProfile

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> =
        repository.login(email.trim().lowercase(), password)
}

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(displayName: String, email: String, password: String): Result<AuthUser> =
        repository.register(displayName.trim(), email.trim().lowercase(), password)
}

/** Signs out: revokes the refresh token when online, then removes all per-user local data. */
class LogoutUseCase(
    private val repository: AuthRepository,
    private val cleaners: List<UserDataCleaner>,
) {
    suspend operator fun invoke() {
        repository.logout()
        cleaners.forEach { it.clearUserData() }
    }
}

/** Deletes the account on the server; local data is removed only after the server confirms. */
class DeleteAccountUseCase(
    private val repository: AuthRepository,
    private val cleaners: List<UserDataCleaner>,
) {
    suspend operator fun invoke(): Result<Unit> =
        repository.deleteAccount().onSuccess { cleaners.forEach { it.clearUserData() } }
}

class RefreshProfileUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<UserProfile> = repository.refreshProfile()
}

class UpdateDisplayNameUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(displayName: String): Result<UserProfile> {
        val trimmed = displayName.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_DISPLAY_NAME_LENGTH) {
            return Result.failure(DataException(DataError.INVALID_INPUT))
        }
        return repository.updateDisplayName(trimmed)
    }

    private companion object {
        const val MAX_DISPLAY_NAME_LENGTH = 80
    }
}
