package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.AppSettingsRepository
import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class AppSettingsRepositoryImpl(private val preferences: AppPreferences) : AppSettingsRepository {
    private val mutableThemeMode = MutableStateFlow(preferences.getThemeMode())
    override val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()

    override fun setThemeMode(mode: ThemeMode) {
        preferences.setThemeMode(mode)
        mutableThemeMode.value = mode
    }

    override fun hasCompletedOnboarding(): Boolean = preferences.hasCompletedOnboarding()

    override fun completeOnboarding() = preferences.setOnboardingCompleted(true)

    override fun recordAppOpen(): Int {
        preferences.incrementAppOpenCount()
        return preferences.getAppOpenCount()
    }

    override fun hasRequestedReview(): Boolean = preferences.hasRequestedReview()

    override fun markReviewRequested() = preferences.setReviewRequested()
}
