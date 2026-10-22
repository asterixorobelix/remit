package com.asterixorobelix.remit.domain.developer

import com.asterixorobelix.remit.testing.FakeSessionRepository
import com.asterixorobelix.remit.testing.RecordingCleaner
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeveloperUseCasesTest {
    private class FakeRepository : DeveloperSettingsRepository {
        var override: String? = "https://old.test"
        var resets = 0
        override fun apiBaseUrlOverride() = override
        override fun setApiBaseUrlOverride(url: String?) {
            override = url
        }
        override fun resetLocalState() {
            resets++
        }
    }

    @Test
    fun acceptsHttpUrlsAndTrimsTrailingSlash() {
        val repository = FakeRepository()
        assertTrue(SetApiBaseUrlOverrideUseCase(repository)(" http://10.0.2.2:8085/ "))
        assertEquals("http://10.0.2.2:8085", repository.override)
    }

    @Test
    fun rejectsInvalidUrlsWithoutChangingTheOverride() {
        val repository = FakeRepository()
        listOf("ftp://x", "api.example.com", "https://", "https://exa mple.com").forEach { url ->
            assertFalse(SetApiBaseUrlOverrideUseCase(repository)(url), url)
        }
        assertEquals("https://old.test", repository.override)
    }

    @Test
    fun blankClearsTheOverride() {
        val repository = FakeRepository()
        assertTrue(SetApiBaseUrlOverrideUseCase(repository)("  "))
        assertNull(repository.override)
    }

    @Test
    fun resetClearsSessionUserDataAndLocalState() = runTest {
        val repository = FakeRepository()
        val session = FakeSessionRepository(signedIn = true)
        val cleaner = RecordingCleaner()
        ResetDevelopmentStateUseCase(repository, session, listOf(cleaner))()
        assertEquals(1, cleaner.calls)
        assertEquals(1, session.clearCalls)
        assertEquals(1, repository.resets)
    }
}
