package com.asterixorobelix.remit.domain.update

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateUseCasesTest {
    private class FakeRepository(var policy: Result<VersionPolicy>) : AppUpdateRepository {
        override suspend fun fetchVersionPolicy() = policy
    }

    @Test
    fun comparesNumericSegments() {
        assertTrue(compareVersions("1.10.0", "1.9.3") > 0)
        assertTrue(compareVersions("1.2", "1.2.1") < 0)
        assertEquals(0, compareVersions("2.0.0", "2.0"))
        assertEquals(0, compareVersions("1.0.0-beta+7", "1.0.0"))
    }

    @Test
    fun requiresUpdateBelowMinimumVersion() = runTest {
        val repository = FakeRepository(Result.success(VersionPolicy("1.3.0", "1.4.0", forceUpdate = false)))
        assertTrue(CheckForceUpdateUseCase(repository)("1.2.9"))
        assertFalse(CheckForceUpdateUseCase(repository)("1.3.0"))
    }

    @Test
    fun forceUpdateFlagRequiresLatestVersion() = runTest {
        val repository = FakeRepository(Result.success(VersionPolicy("0.0.0", "2.0.0", forceUpdate = true)))
        assertTrue(CheckForceUpdateUseCase(repository)("1.9.0"))
        assertFalse(CheckForceUpdateUseCase(repository)("2.0.0"))
    }

    @Test
    fun networkFailureNeverBlocksTheApp() = runTest {
        val repository = FakeRepository(Result.failure(IllegalStateException("offline")))
        assertFalse(CheckForceUpdateUseCase(repository)("0.0.1"))
    }
}
