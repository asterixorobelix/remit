package com.asterixorobelix.remit.domain.settings

import kotlinx.coroutines.flow.StateFlow

/** Device-local app settings and lifecycle flags. */
interface AppSettingsRepository {
    val themeMode: StateFlow<ThemeMode>
    fun setThemeMode(mode: ThemeMode)

    fun hasCompletedOnboarding(): Boolean
    fun completeOnboarding()

    /** Records an app launch and returns the total number of launches. */
    fun recordAppOpen(): Int
    fun hasRequestedReview(): Boolean
    fun markReviewRequested()
}
