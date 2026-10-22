package com.asterixorobelix.remit.domain.settings

import com.asterixorobelix.remit.testing.FakeAppSettingsRepository
import com.asterixorobelix.remit.testing.FakeSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsUseCasesTest {
    @Test
    fun reviewPromptIsRequestedOnlyOnTheFifthOpen() {
        val repository = FakeAppSettingsRepository()
        val recordOpen = RecordAppOpenUseCase(repository)
        val prompts = (1..7).map { recordOpen() }
        assertEquals(listOf(false, false, false, false, true, false, false), prompts)
    }

    @Test
    fun reviewPromptIsSkippedWhenAlreadyRequested() {
        val repository = FakeAppSettingsRepository(opens = 4, reviewRequested = true)
        assertEquals(false, RecordAppOpenUseCase(repository)())
    }

    @Test
    fun startDestinationFollowsOnboardingThenSession() = runTest {
        val session = FakeSessionRepository(signedIn = false)
        val notOnboarded = ResolveStartDestinationUseCase(FakeAppSettingsRepository(onboardingDone = false), session)
        assertEquals(StartDestination.ONBOARDING, notOnboarded(authEnabled = true))

        val onboarded = ResolveStartDestinationUseCase(FakeAppSettingsRepository(onboardingDone = true), session)
        assertEquals(StartDestination.LOGIN, onboarded(authEnabled = true))
        assertEquals(StartDestination.HOME, onboarded(authEnabled = false))

        session.signedIn = true
        assertEquals(StartDestination.HOME, onboarded(authEnabled = true))
    }
}
