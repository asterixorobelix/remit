package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode
import platform.Foundation.NSUserDefaults

class IOSAppPreferences : AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun hasCompletedOnboarding(): Boolean =
        defaults.boolForKey(KEY_ONBOARDING_COMPLETED)

    override fun setOnboardingCompleted(completed: Boolean) {
        defaults.setBool(completed, KEY_ONBOARDING_COMPLETED)
    }

    override fun getThemeMode(): ThemeMode {
        val value = defaults.stringForKey(KEY_THEME_MODE)
        return ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
    }

    override fun setThemeMode(mode: ThemeMode) {
        defaults.setObject(mode.name, KEY_THEME_MODE)
    }

    override fun getAppOpenCount(): Int =
        defaults.integerForKey(KEY_APP_OPEN_COUNT).toInt()

    override fun incrementAppOpenCount() {
        val current = getAppOpenCount()
        defaults.setInteger(current.toLong() + 1, KEY_APP_OPEN_COUNT)
    }

    override fun hasRequestedReview(): Boolean =
        defaults.boolForKey(KEY_REVIEW_REQUESTED)

    override fun setReviewRequested() {
        defaults.setBool(true, KEY_REVIEW_REQUESTED)
    }

    override fun getApiBaseUrlOverride(): String? = defaults.stringForKey(KEY_API_OVERRIDE)
    override fun setApiBaseUrlOverride(url: String?) {
        if (url == null) defaults.removeObjectForKey(KEY_API_OVERRIDE) else defaults.setObject(url, KEY_API_OVERRIDE)
    }
    override fun resetForDevelopment() {
        listOf(
            KEY_ONBOARDING_COMPLETED,
            KEY_THEME_MODE,
            KEY_APP_OPEN_COUNT,
            KEY_REVIEW_REQUESTED,
            KEY_API_OVERRIDE,
        ).forEach(defaults::removeObjectForKey)
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_OPEN_COUNT = "app_open_count"
        private const val KEY_REVIEW_REQUESTED = "review_requested"
        private const val KEY_API_OVERRIDE = "api_url_override"
    }
}

actual fun createAppPreferences(): AppPreferences = IOSAppPreferences()
