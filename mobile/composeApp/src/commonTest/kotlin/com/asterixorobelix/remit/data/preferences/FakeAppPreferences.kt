package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode

class FakeAppPreferences : AppPreferences {
    private var onboardingCompleted = false
    private var themeMode = ThemeMode.SYSTEM
    private var appOpenCount = 0
    private var reviewRequested = false
    private var apiOverride: String? = null

    override fun hasCompletedOnboarding() = onboardingCompleted
    override fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }
    override fun getThemeMode() = themeMode
    override fun setThemeMode(mode: ThemeMode) {
        themeMode = mode
    }
    override fun getAppOpenCount() = appOpenCount
    override fun incrementAppOpenCount() {
        appOpenCount++
    }
    override fun hasRequestedReview() = reviewRequested
    override fun setReviewRequested() {
        reviewRequested = true
    }
    override fun getApiBaseUrlOverride(): String? = apiOverride
    override fun setApiBaseUrlOverride(url: String?) {
        apiOverride = url
    }
    override fun resetForDevelopment() {
        onboardingCompleted = false
        themeMode = ThemeMode.SYSTEM
        appOpenCount = 0
        reviewRequested = false
        apiOverride = null
    }
}
