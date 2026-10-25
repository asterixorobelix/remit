package com.asterixorobelix.remit.domain.settings

import kotlinx.coroutines.flow.Flow

/** Device-local app settings and lifecycle flags. */
interface AppSettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun completeOnboarding()

    /** Records an app launch and returns the total number of launches. */
    suspend fun recordAppOpen(): Int
    suspend fun hasRequestedReview(): Boolean
    suspend fun markReviewRequested()
}
