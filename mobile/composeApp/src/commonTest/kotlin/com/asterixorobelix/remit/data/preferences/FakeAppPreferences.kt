package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAppPreferences : AppPreferences {
    private val mutableThemeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = mutableThemeMode.asStateFlow()
    private var onboardingCompleted = false
    private var appOpenCount = 0
    private var reviewRequested = false
    private var apiOverride: String? = null

    override suspend fun setThemeMode(mode: ThemeMode) {
        mutableThemeMode.value = mode
    }
    override suspend fun hasCompletedOnboarding() = onboardingCompleted
    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }
    override suspend fun incrementAppOpenCount() = ++appOpenCount
    override suspend fun hasRequestedReview() = reviewRequested
    override suspend fun setReviewRequested() {
        reviewRequested = true
    }
    override suspend fun getApiBaseUrlOverride(): String? = apiOverride
    override suspend fun setApiBaseUrlOverride(url: String?) {
        apiOverride = url
    }
    override suspend fun resetForDevelopment() {
        onboardingCompleted = false
        mutableThemeMode.value = ThemeMode.SYSTEM
        appOpenCount = 0
        reviewRequested = false
        apiOverride = null
    }
}
