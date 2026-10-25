package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The real DataStore on a temporary file: what the app reads after a restart. */
class DataStoreAppPreferencesTest {
    private val file = createTempDirectory("remit-settings").resolve(DataStoreAppPreferences.FILE_NAME).toString()

    /** Runs [block] against a store on [file], then closes it as a process exit would. */
    private suspend fun <T> withStore(block: suspend (AppPreferences) -> T): T {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        return try {
            block(DataStoreAppPreferences(createPreferencesDataStore(file, scope = scope)))
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun aFreshInstallHasTheDefaults() = runTest {
        withStore { preferences ->
            assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
            assertFalse(preferences.hasCompletedOnboarding())
            assertFalse(preferences.hasRequestedReview())
            assertNull(preferences.getApiBaseUrlOverride())
        }
    }

    @Test
    fun settingsSurviveARestart() = runTest {
        withStore { preferences ->
            preferences.setThemeMode(ThemeMode.DARK)
            preferences.setOnboardingCompleted(true)
            preferences.setReviewRequested()
            preferences.setApiBaseUrlOverride("http://10.0.2.2:8085")
            assertEquals(1, preferences.incrementAppOpenCount())
        }

        withStore { preferences ->
            assertEquals(ThemeMode.DARK, preferences.themeMode.first())
            assertTrue(preferences.hasCompletedOnboarding())
            assertTrue(preferences.hasRequestedReview())
            assertEquals("http://10.0.2.2:8085", preferences.getApiBaseUrlOverride())
            assertEquals(2, preferences.incrementAppOpenCount())
        }
    }

    @Test
    fun clearingTheOverrideAndResettingRemoveTheValues() = runTest {
        withStore { preferences ->
            preferences.setApiBaseUrlOverride("http://10.0.2.2:8085")
            preferences.setApiBaseUrlOverride(null)
            assertNull(preferences.getApiBaseUrlOverride())

            preferences.setThemeMode(ThemeMode.LIGHT)
            preferences.setOnboardingCompleted(true)
            preferences.resetForDevelopment()

            assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
            assertFalse(preferences.hasCompletedOnboarding())
        }
    }

    @Test
    fun aCorruptFileStartsOverWithTheDefaults() = runTest {
        File(file).apply { parentFile.mkdirs() }.writeText("not a preferences file")

        withStore { preferences ->
            assertEquals(ThemeMode.SYSTEM, preferences.themeMode.first())
            preferences.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, preferences.themeMode.first())
        }
    }
}
