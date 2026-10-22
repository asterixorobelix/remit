package com.example.myproject.data.preferences

import com.example.myproject.domain.settings.ThemeMode

class FakeAppPreferences : AppPreferences {
    private var onboardingCompleted = false
    private var themeMode = ThemeMode.SYSTEM
    private var appOpenCount = 0
    private var reviewRequested = false
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var userDisplayName: String? = null
    private var userEmail: String? = null
    private var apiOverride: String? = null
    private val recentSearches = mutableListOf<String>()

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

    override fun getAccessToken() = accessToken
    override fun setAccessToken(token: String?) {
        accessToken = token
    }
    override fun getRefreshToken() = refreshToken
    override fun setRefreshToken(token: String?) {
        refreshToken = token
    }
    override fun clearAuthTokens() {
        accessToken = null
        refreshToken = null
    }

    override fun getUserDisplayName() = userDisplayName
    override fun setUserDisplayName(name: String?) {
        userDisplayName = name
    }
    override fun getUserEmail() = userEmail
    override fun setUserEmail(email: String?) {
        userEmail = email
    }

    override fun getRecentSearches(): List<String> = recentSearches.toList()
    override fun addRecentSearch(query: String) {
        recentSearches.remove(query)
        recentSearches.add(0, query)
    }
    override fun clearRecentSearches() {
        recentSearches.clear()
    }
    override fun getApiBaseUrlOverride(): String? = apiOverride
    override fun setApiBaseUrlOverride(url: String?) {
        apiOverride = url
    }
    override fun resetForDevelopment() {
        onboardingCompleted = false
        apiOverride = null
        clearAuthTokens()
        clearRecentSearches()
    }
}
