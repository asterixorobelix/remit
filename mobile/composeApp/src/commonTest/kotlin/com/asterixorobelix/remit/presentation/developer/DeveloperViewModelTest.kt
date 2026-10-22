package com.asterixorobelix.remit.presentation.developer

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.developer.DeveloperSettingsRepository
import com.asterixorobelix.remit.domain.developer.GetApiBaseUrlOverrideUseCase
import com.asterixorobelix.remit.domain.developer.ResetDevelopmentStateUseCase
import com.asterixorobelix.remit.domain.developer.SetApiBaseUrlOverrideUseCase
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.developer_invalid_url
import com.asterixorobelix.remit.generated.resources.developer_saved
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.testing.FakeAppConfig
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DeveloperViewModelTest {
    private class FakeRepository(var override: String? = null) : DeveloperSettingsRepository {
        var resets = 0
        override fun apiBaseUrlOverride() = override
        override fun setApiBaseUrlOverride(url: String?) {
            override = url
        }
        override fun resetLocalState() {
            resets++
        }
    }

    private val config = FakeAppConfig(capabilities = setOf(Capability.BACKEND), apiBaseUrl = "https://api.test")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakeRepository) = DeveloperViewModel(
        appConfig = config,
        getApiBaseUrlOverride = GetApiBaseUrlOverrideUseCase(repository),
        setApiBaseUrlOverride = SetApiBaseUrlOverrideUseCase(repository),
        resetDevelopmentState = ResetDevelopmentStateUseCase(repository),
    )

    private fun DeveloperViewModel.content() = uiState.value as DeveloperUiState.Content

    @Test
    fun showsBuildInfoAndTheCurrentOverride() {
        val content = viewModel(FakeRepository(override = "http://10.0.2.2:8085")).content()
        assertEquals("DEVELOPMENT", content.environment)
        assertEquals("1.2.0", content.versionName)
        assertEquals("backend", content.capabilities)
        assertEquals("https://api.test", content.defaultApiUrl)
        assertEquals("http://10.0.2.2:8085", content.apiUrlDraft)
    }

    @Test
    fun draftStartsAtTheBuildDefaultWithoutAnOverride() {
        assertEquals("https://api.test", viewModel(FakeRepository()).content().apiUrlDraft)
    }

    @Test
    fun invalidUrlShowsAnErrorUntilEdited() {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)

        viewModel.onEvent(DeveloperEvent.ApiUrlChanged("not a url"))
        viewModel.onEvent(DeveloperEvent.SaveApiUrl)
        assertEquals(Res.string.developer_invalid_url, viewModel.content().urlError)
        assertNull(repository.override)

        viewModel.onEvent(DeveloperEvent.ApiUrlChanged("http://localhost:8085"))
        assertNull(viewModel.content().urlError)
    }

    @Test
    fun validUrlIsSavedAndConfirmed() = runTest {
        val repository = FakeRepository()
        val viewModel = viewModel(repository)
        viewModel.effects.test {
            viewModel.onEvent(DeveloperEvent.ApiUrlChanged("http://localhost:8085/"))
            viewModel.onEvent(DeveloperEvent.SaveApiUrl)
            assertEquals(DeveloperEffect.ShowMessage(Res.string.developer_saved), awaitItem())
        }
        assertEquals("http://localhost:8085", repository.override)
    }

    @Test
    fun savingTheBuildDefaultRemovesTheOverride() {
        val repository = FakeRepository(override = "http://localhost:8085")
        val viewModel = viewModel(repository)

        viewModel.onEvent(DeveloperEvent.ApiUrlChanged(" https://api.test "))
        viewModel.onEvent(DeveloperEvent.SaveApiUrl)

        assertNull(repository.override)
    }

    @Test
    fun resetClearsLocalStateAndRestartsTheApp() = runTest {
        val repository = FakeRepository(override = "http://localhost:8085")
        val viewModel = viewModel(repository)
        viewModel.effects.test {
            viewModel.onEvent(DeveloperEvent.ResetApp)
            assertEquals(DeveloperEffect.AppReset, awaitItem())
        }
        assertEquals(1, repository.resets)
    }
}
