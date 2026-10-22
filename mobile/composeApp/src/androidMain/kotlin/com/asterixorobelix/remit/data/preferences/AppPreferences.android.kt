package com.asterixorobelix.remit.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.asterixorobelix.remit.domain.settings.ThemeMode

class AndroidAppPreferences(private val prefs: SharedPreferences) : AppPreferences {

    override fun hasCompletedOnboarding(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
    }

    override fun getThemeMode(): ThemeMode {
        val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    override fun getAppOpenCount(): Int =
        prefs.getInt(KEY_APP_OPEN_COUNT, 0)

    override fun incrementAppOpenCount() {
        val current = getAppOpenCount()
        prefs.edit { putInt(KEY_APP_OPEN_COUNT, current + 1) }
    }

    override fun hasRequestedReview(): Boolean =
        prefs.getBoolean(KEY_REVIEW_REQUESTED, false)

    override fun setReviewRequested() {
        prefs.edit { putBoolean(KEY_REVIEW_REQUESTED, true) }
    }

    override fun getApiBaseUrlOverride(): String? = prefs.getString(KEY_API_OVERRIDE, null)
    override fun setApiBaseUrlOverride(url: String?) {
        prefs.edit { putString(KEY_API_OVERRIDE, url) }
    }
    override fun resetForDevelopment() {
        prefs.edit { clear() }
    }

    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_OPEN_COUNT = "app_open_count"
        private const val KEY_REVIEW_REQUESTED = "review_requested"
        private const val KEY_API_OVERRIDE = "api_url_override"

        private lateinit var instance: SharedPreferences

        fun init(context: Context) {
            instance = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun getSharedPreferences(): SharedPreferences = instance
    }
}

actual fun createAppPreferences(): AppPreferences =
    AndroidAppPreferences(prefs = AndroidAppPreferences.getSharedPreferences())
