package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.AppSettingsRepository
import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.coroutines.flow.Flow

internal class AppSettingsRepositoryImpl(private val preferences: AppPreferences) : AppSettingsRepository {
    override val themeMode: Flow<ThemeMode> = preferences.themeMode

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    override suspend fun hasCompletedOnboarding(): Boolean = preferences.hasCompletedOnboarding()

    override suspend fun completeOnboarding() = preferences.setOnboardingCompleted(true)

    override suspend fun recordAppOpen(): Int = preferences.incrementAppOpenCount()

    override suspend fun hasRequestedReview(): Boolean = preferences.hasRequestedReview()

    override suspend fun markReviewRequested() = preferences.setReviewRequested()
}
