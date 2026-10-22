package com.asterixorobelix.remit.presentation.app

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.settings.MarkReviewRequestedUseCase
import com.asterixorobelix.remit.domain.settings.ObserveThemeModeUseCase
import com.asterixorobelix.remit.domain.settings.RecordAppOpenUseCase
import com.asterixorobelix.remit.domain.settings.ResolveStartDestinationUseCase
import com.asterixorobelix.remit.domain.settings.StartDestination
import com.asterixorobelix.remit.domain.update.AppUpdateRepository
import com.asterixorobelix.remit.domain.update.CheckForceUpdateUseCase
import com.asterixorobelix.remit.domain.update.VersionPolicy
import com.asterixorobelix.remit.testing.FakeAppConfig
import com.asterixorobelix.remit.testing.FakeAppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        settings: FakeAppSettingsRepository = FakeAppSettingsRepository(onboardingDone = true),
        policy: VersionPolicy? = null,
    ) = AppViewModel(
        appConfig = FakeAppConfig(versionName = "1.0.0"),
        observeThemeMode = ObserveThemeModeUseCase(settings),
        launch = AppLaunchUseCases(
            resolveStartDestination = ResolveStartDestinationUseCase(settings),
            recordAppOpen = RecordAppOpenUseCase(settings),
            markReviewRequested = MarkReviewRequestedUseCase(settings),
        ),
        checkForceUpdate = policy?.let { value ->
            CheckForceUpdateUseCase(
                object : AppUpdateRepository {
                    override suspend fun fetchVersionPolicy() = Result.success(value)
                },
            )
        },
    )

    @Test
    fun startsAtHomeOnceOnboardingIsDone() {
        assertEquals(AppUiState.Ready(StartDestination.HOME), viewModel().uiState.value)
    }

    @Test
    fun startsAtOnboardingOnFirstLaunch() {
        val state = viewModel(FakeAppSettingsRepository(onboardingDone = false)).uiState.value
        assertEquals(AppUiState.Ready(StartDestination.ONBOARDING), state)
    }

    @Test
    fun blocksWithForceUpdateBelowMinimumVersion() {
        val state = viewModel(policy = VersionPolicy("2.0.0", "2.0.0", forceUpdate = false)).uiState.value
        assertEquals(AppUiState.Ready(StartDestination.HOME, updateRequired = true), state)
    }

    @Test
    fun reviewIsRequestedOnTheFifthOpenAndRecorded() = runTest {
        val settings = FakeAppSettingsRepository(opens = 4)
        val viewModel = viewModel(settings)
        viewModel.effects.test {
            assertEquals(AppEffect.RequestReview, awaitItem())
        }
        viewModel.onReviewFlowFinished()
        assertTrue(settings.hasRequestedReview())
    }
}
