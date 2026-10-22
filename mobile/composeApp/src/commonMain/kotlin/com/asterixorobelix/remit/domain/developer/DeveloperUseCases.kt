package com.asterixorobelix.remit.domain.developer

import com.asterixorobelix.remit.domain.session.SessionRepository
import com.asterixorobelix.remit.domain.session.UserDataCleaner

/** Debug-only overrides. Production builds never expose these controls. */
interface DeveloperSettingsRepository {
    fun apiBaseUrlOverride(): String?
    fun setApiBaseUrlOverride(url: String?)

    /** Clears onboarding, theme, search history and overrides. */
    fun resetLocalState()
}

private val HTTP_URL = Regex("^https?://[^\\s/?#]+(:\\d{1,5})?(/[^\\s]*)?$", RegexOption.IGNORE_CASE)

class GetApiBaseUrlOverrideUseCase(private val repository: DeveloperSettingsRepository) {
    operator fun invoke(): String? = repository.apiBaseUrlOverride()
}

/** Saves an http(s) base URL; blank clears the override. Returns false for invalid URLs. */
class SetApiBaseUrlOverrideUseCase(private val repository: DeveloperSettingsRepository) {
    operator fun invoke(url: String): Boolean {
        val trimmed = url.trim().trimEnd('/')
        return when {
            trimmed.isEmpty() -> {
                repository.setApiBaseUrlOverride(null)
                true
            }

            HTTP_URL.matches(trimmed) -> {
                repository.setApiBaseUrlOverride(trimmed)
                true
            }

            else -> false
        }
    }
}

/** Returns the app to a first-launch state: signed out, no per-user data, onboarding shown. */
class ResetDevelopmentStateUseCase(
    private val repository: DeveloperSettingsRepository,
    private val session: SessionRepository,
    private val cleaners: List<UserDataCleaner>,
) {
    suspend operator fun invoke() {
        cleaners.forEach { it.clearUserData() }
        session.clearLocalSession()
        repository.resetLocalState()
    }
}
