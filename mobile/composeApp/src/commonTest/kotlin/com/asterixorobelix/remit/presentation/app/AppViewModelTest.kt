package com.asterixorobelix.remit.presentation.app

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.session.ObserveSessionExpiredUseCase
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
import com.asterixorobelix.remit.testing.FakeSessionRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val settings = FakeAppSettingsRepository(onboardingDone = true)
    private val session = FakeSessionRepository(signedIn = false)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(policy: VersionPolicy? = null) = AppViewModel(
        appConfig = FakeAppConfig(versionName = "1.0.0"),
        observeThemeMode = ObserveThemeModeUseCase(settings),
        launch = AppLaunchUseCases(
            resolveStartDestination = ResolveStartDestinationUseCase(settings, session),
            recordAppOpen = RecordAppOpenUseCase(settings),
            markReviewRequested = MarkReviewRequestedUseCase(settings),
            observeSessionExpired = ObserveSessionExpiredUseCase(session),
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
    fun resolvesSignInWhenAuthEnabledAndSignedOut() {
        assertEquals(AppUiState.Ready(StartDestination.LOGIN), viewModel().uiState.value)
    }

    @Test
    fun blocksWithForceUpdateBelowMinimumVersion() {
        val state = viewModel(VersionPolicy("2.0.0", "2.0.0", forceUpdate = false)).uiState.value
        assertEquals(AppUiState.Ready(StartDestination.LOGIN, updateRequired = true), state)
    }

    @Test
    fun sessionExpiryIsForwardedAsEffect() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            session.expiredEvents.emit(Unit)
            assertEquals(AppEffect.SessionExpired, awaitItem())
        }
    }
}
