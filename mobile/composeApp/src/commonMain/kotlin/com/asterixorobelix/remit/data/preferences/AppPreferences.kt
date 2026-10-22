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

    // Auth
    fun getAccessToken(): String?
    fun setAccessToken(token: String?)
    fun getRefreshToken(): String?
    fun setRefreshToken(token: String?)
    fun clearAuthTokens()

    // User profile cache
    fun getUserDisplayName(): String?
    fun setUserDisplayName(name: String?)
    fun getUserEmail(): String?
    fun setUserEmail(email: String?)

    // Recent searches
    fun getRecentSearches(): List<String>
    fun addRecentSearch(query: String)
    fun clearRecentSearches()

    // Debug-only overrides. Production UI must never expose these controls.
    fun getApiBaseUrlOverride(): String?
    fun setApiBaseUrlOverride(url: String?)
    fun resetForDevelopment()
}

expect fun createAppPreferences(): AppPreferences
