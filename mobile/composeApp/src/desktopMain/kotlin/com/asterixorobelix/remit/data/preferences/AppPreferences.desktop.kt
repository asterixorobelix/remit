package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode

private class DesktopAppPreferences : AppPreferences {
    private val values = mutableMapOf<String, Any?>()
    override fun hasCompletedOnboarding() = values["onboarding"] as? Boolean ?: false
    override fun setOnboardingCompleted(completed: Boolean) {
        values["onboarding"] = completed
    }
    override fun getThemeMode() = values["theme"] as? ThemeMode ?: ThemeMode.SYSTEM
    override fun setThemeMode(mode: ThemeMode) {
        values["theme"] = mode
    }
    override fun getAppOpenCount() = values["opens"] as? Int ?: 0
    override fun incrementAppOpenCount() {
        values["opens"] = getAppOpenCount() + 1
    }
    override fun hasRequestedReview() = values["review"] as? Boolean ?: false
    override fun setReviewRequested() {
        values["review"] = true
    }
    override fun getApiBaseUrlOverride() = values["api"] as? String
    override fun setApiBaseUrlOverride(url: String?) {
        values["api"] = url
    }
    override fun resetForDevelopment() {
        values.clear()
    }
}

actual fun createAppPreferences(): AppPreferences = DesktopAppPreferences()
