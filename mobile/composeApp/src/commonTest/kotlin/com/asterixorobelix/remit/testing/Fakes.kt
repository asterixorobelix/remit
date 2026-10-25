package com.asterixorobelix.remit.testing

import com.asterixorobelix.remit.domain.settings.AppSettingsRepository
import com.asterixorobelix.remit.domain.settings.ThemeMode
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.platform.Environment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FakeAppConfig(
    override val capabilities: Set<Capability> = setOf(Capability.BACKEND),
    override val environment: Environment = Environment.DEVELOPMENT,
    override val apiBaseUrl: String = "https://api.test",
    override val isDebug: Boolean = true,
    override val versionName: String = "1.2.0",
    override val versionCode: Int = 10200,
    override val applicationId: String = "com.asterixorobelix.remit",
    override val firebaseEmulatorHost: String? = null,
) : AppConfig

class FakeAppSettingsRepository(
    private var onboardingDone: Boolean = true,
    private var opens: Int = 0,
    private var reviewRequested: Boolean = false,
) : AppSettingsRepository {
    private val mutableTheme = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = mutableTheme.asStateFlow()
    override suspend fun setThemeMode(mode: ThemeMode) {
        mutableTheme.value = mode
    }
    override suspend fun hasCompletedOnboarding(): Boolean = onboardingDone
    override suspend fun completeOnboarding() {
        onboardingDone = true
    }
    override suspend fun recordAppOpen(): Int = ++opens
    override suspend fun hasRequestedReview(): Boolean = reviewRequested
    override suspend fun markReviewRequested() {
        reviewRequested = true
    }
}
