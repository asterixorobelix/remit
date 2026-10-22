package com.example.myproject.presentation.settings

import app.cash.turbine.test
import com.example.myproject.domain.settings.MarkReviewRequestedUseCase
import com.example.myproject.domain.settings.ObserveThemeModeUseCase
import com.example.myproject.domain.settings.SetThemeModeUseCase
import com.example.myproject.domain.settings.ThemeMode
import com.example.myproject.platform.Capability
import com.example.myproject.testing.FakeAppConfig
import com.example.myproject.testing.FakeAppSettingsRepository
import com.example.myproject.testing.awaitItemOfType
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val repository = FakeAppSettingsRepository()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(config: FakeAppConfig) = SettingsViewModel(
        appConfig = config,
        observeThemeMode = ObserveThemeModeUseCase(repository),
        setThemeMode = SetThemeModeUseCase(repository),
        markReviewRequested = MarkReviewRequestedUseCase(repository),
    )

    @Test
    fun capabilityGatedEntriesFollowConfig() = runTest {
        val minimal = viewModel(FakeAppConfig(capabilities = emptySet(), isDebug = false))
        minimal.uiState.test {
            val content = awaitItemOfType<SettingsUiState.Content>()
            assertFalse(content.showAccount)
            assertFalse(content.showPremium)
            assertFalse(content.showDeveloperTools)
        }
        val full = viewModel(FakeAppConfig(capabilities = setOf(Capability.AUTH, Capability.BILLING)))
        full.uiState.test {
            val content = awaitItemOfType<SettingsUiState.Content>()
            assertTrue(content.showAccount)
            assertTrue(content.showPremium)
            assertTrue(content.showDeveloperTools)
            assertEquals("1.2.0", content.appVersion)
        }
    }

    @Test
    fun themeSelectionIsPersistedAndReflected() = runTest {
        val viewModel = viewModel(FakeAppConfig())
        viewModel.uiState.test {
            assertEquals(ThemeMode.SYSTEM, awaitItemOfType<SettingsUiState.Content>().themeMode)
            viewModel.onEvent(SettingsEvent.ThemeModeSelected(ThemeMode.DARK))
            assertEquals(ThemeMode.DARK, awaitItemOfType<SettingsUiState.Content>().themeMode)
        }
        assertEquals(ThemeMode.DARK, repository.themeMode.value)
    }

    @Test
    fun rateAppRequestsReviewOnceAndRecordsIt() = runTest {
        val viewModel = viewModel(FakeAppConfig())
        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.RateAppClicked)
            assertEquals(SettingsEffect.RequestReview, awaitItem())
        }
        viewModel.onEvent(SettingsEvent.ReviewFlowFinished)
        assertTrue(repository.hasRequestedReview())
    }
}
