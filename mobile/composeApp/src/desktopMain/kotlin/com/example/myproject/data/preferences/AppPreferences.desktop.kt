package com.example.myproject.data.preferences

import com.example.myproject.domain.settings.ThemeMode

private const val MAX_RECENT_SEARCHES = 10

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
    override fun getAccessToken() = values["access"] as? String
    override fun setAccessToken(token: String?) {
        values["access"] = token
    }
    override fun getRefreshToken() = values["refresh"] as? String
    override fun setRefreshToken(token: String?) {
        values["refresh"] = token
    }
    override fun clearAuthTokens() {
        values.remove("access")
        values.remove("refresh")
    }
    override fun getUserDisplayName() = values["name"] as? String
    override fun setUserDisplayName(name: String?) {
        values["name"] = name
    }
    override fun getUserEmail() = values["email"] as? String
    override fun setUserEmail(email: String?) {
        values["email"] = email
    }

    @Suppress("UNCHECKED_CAST")
    override fun getRecentSearches() = values["searches"] as? List<String> ?: emptyList()
    override fun addRecentSearch(query: String) {
        values["searches"] =
            (listOf(query) + getRecentSearches()).distinct().take(MAX_RECENT_SEARCHES)
    }
    override fun clearRecentSearches() {
        values.remove("searches")
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
