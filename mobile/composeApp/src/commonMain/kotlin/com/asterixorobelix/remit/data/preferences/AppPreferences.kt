package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode

interface AppPreferences {
    fun hasCompletedOnboarding(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    fun getThemeMode(): ThemeMode
    fun setThemeMode(mode: ThemeMode)
    fun getAppOpenCount(): Int
    fun incrementAppOpenCount()
    fun hasRequestedReview(): Boolean
    fun setReviewRequested()

    // Debug-only overrides. Production UI must never expose these controls.
    fun getApiBaseUrlOverride(): String?
    fun setApiBaseUrlOverride(url: String?)
    fun resetForDevelopment()
}

expect fun createAppPreferences(): AppPreferences
