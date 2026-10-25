package com.asterixorobelix.remit.domain.developer

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
        override suspend fun apiBaseUrlOverride() = override
        override suspend fun setApiBaseUrlOverride(url: String?) {
            override = url
        }
        override suspend fun resetLocalState() {
            resets++
        }
    }

    @Test
    fun acceptsHttpUrlsAndTrimsTrailingSlash() = runTest {
        val repository = FakeRepository()
        assertTrue(SetApiBaseUrlOverrideUseCase(repository)(" http://10.0.2.2:8085/ "))
        assertEquals("http://10.0.2.2:8085", repository.override)
    }

    @Test
    fun rejectsInvalidUrlsWithoutChangingTheOverride() = runTest {
        val repository = FakeRepository()
        listOf("ftp://x", "api.example.com", "https://", "https://exa mple.com").forEach { url ->
            assertFalse(SetApiBaseUrlOverrideUseCase(repository)(url), url)
        }
        assertEquals("https://old.test", repository.override)
    }

    @Test
    fun blankClearsTheOverride() = runTest {
        val repository = FakeRepository()
        assertTrue(SetApiBaseUrlOverrideUseCase(repository)("  "))
        assertNull(repository.override)
    }

    @Test
    fun resetClearsLocalState() = runTest {
        val repository = FakeRepository()
        ResetDevelopmentStateUseCase(repository)()
        assertEquals(1, repository.resets)
    }
}
