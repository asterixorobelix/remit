package com.asterixorobelix.remit.domain.settings

import com.asterixorobelix.remit.testing.FakeAppSettingsRepository
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
    fun startDestinationIsOnboardingUntilCompleted() {
        val repository = FakeAppSettingsRepository(onboardingDone = false)
        val resolve = ResolveStartDestinationUseCase(repository)
        assertEquals(StartDestination.ONBOARDING, resolve())

        CompleteOnboardingUseCase(repository)()
        assertEquals(StartDestination.HOME, resolve())
    }
}
