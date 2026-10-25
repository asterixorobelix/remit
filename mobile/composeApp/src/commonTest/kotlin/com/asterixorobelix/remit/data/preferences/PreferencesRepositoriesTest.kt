package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.data.developer.PreferencesDeveloperSettingsRepository
import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreferencesRepositoriesTest {
    private val preferences = FakeAppPreferences()

    @Test
    fun settingsArePersistedAndThemeIsObservable() = runTest {
        val repository = AppSettingsRepositoryImpl(preferences)
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        assertFalse(repository.hasCompletedOnboarding())
        repository.completeOnboarding()
        assertTrue(repository.hasCompletedOnboarding())

        assertEquals(1, repository.recordAppOpen())
        assertEquals(2, repository.recordAppOpen())
        assertFalse(repository.hasRequestedReview())
        repository.markReviewRequested()
        assertTrue(repository.hasRequestedReview())
    }

    @Test
    fun developerSettingsStoreTheOverrideAndResetEverything() = runTest {
        val repository = PreferencesDeveloperSettingsRepository(preferences)
        repository.setApiBaseUrlOverride("http://localhost:8085")
        preferences.setOnboardingCompleted(true)
        assertEquals("http://localhost:8085", repository.apiBaseUrlOverride())

        repository.resetLocalState()

        assertNull(repository.apiBaseUrlOverride())
        assertFalse(preferences.hasCompletedOnboarding())
    }
}
