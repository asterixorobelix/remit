package com.example.myproject.presentation.profile

import app.cash.turbine.test
import com.example.myproject.domain.auth.DeleteAccountUseCase
import com.example.myproject.domain.auth.LogoutUseCase
import com.example.myproject.domain.auth.RefreshProfileUseCase
import com.example.myproject.domain.auth.UpdateDisplayNameUseCase
import com.example.myproject.domain.common.DataError
import com.example.myproject.domain.session.ComputeInitialsUseCase
import com.example.myproject.domain.session.GetCachedProfileUseCase
import com.example.myproject.domain.session.UserProfile
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.message_error_network
import com.example.myproject.testing.FakeAuthRepository
import com.example.myproject.testing.FakeProfileCache
import com.example.myproject.testing.RecordingCleaner
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val repository = FakeAuthRepository()
    private val cache = FakeProfileCache(UserProfile("John Doe", "john@example.com"))
    private val cleaner = RecordingCleaner()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ProfileViewModel(
        getCachedProfile = GetCachedProfileUseCase(cache),
        refreshProfile = RefreshProfileUseCase(repository),
        computeInitials = ComputeInitialsUseCase(),
        updateDisplayName = UpdateDisplayNameUseCase(repository),
        logout = LogoutUseCase(repository, listOf(cleaner)),
        deleteAccount = DeleteAccountUseCase(repository, listOf(cleaner)),
    )

    @Test
    fun showsCachedProfileThenServerProfile() {
        repository.profile = UserProfile("Jane Roe", "jane@example.com")
        val state = assertIs<ProfileUiState.Content>(viewModel().uiState.value)
        assertEquals("Jane Roe", state.profile.displayName)
        assertEquals("JR", state.initials)
    }

    @Test
    fun keepsCachedProfileWhenOffline() {
        repository.failure = DataError.NETWORK
        val state = assertIs<ProfileUiState.Content>(viewModel().uiState.value)
        assertEquals("John Doe", state.profile.displayName)
    }

    @Test
    fun showsErrorWithoutCacheAndRetries() {
        cache.profile = null
        repository.failure = DataError.NETWORK
        val viewModel = viewModel()
        assertEquals(Res.string.message_error_network, assertIs<ProfileUiState.Error>(viewModel.uiState.value).message)

        repository.failure = null
        viewModel.onEvent(ProfileEvent.Retry)
        assertIs<ProfileUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun signOutLogsOutClearsUserDataAndNavigates() = runTest {
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.SignOut)
            assertEquals(ProfileEffect.SignedOut, awaitItem())
        }
        assertEquals(1, repository.logoutCalls)
        assertEquals(1, cleaner.calls)
    }

    @Test
    fun deleteAccountRequiresConfirmation() = runTest {
        val viewModel = viewModel()
        viewModel.onEvent(ProfileEvent.DeleteAccountClicked)
        assertEquals(ProfileDialog.ConfirmDelete, assertIs<ProfileUiState.Content>(viewModel.uiState.value).dialog)
        assertEquals(0, repository.deleteCalls)

        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.ConfirmDeleteAccount)
            assertEquals(ProfileEffect.SignedOut, awaitItem())
        }
        assertEquals(1, repository.deleteCalls)
        assertEquals(1, cleaner.calls)
    }

    @Test
    fun deleteAccountFailureKeepsSessionAndShowsMessage() = runTest {
        val viewModel = viewModel()
        repository.failure = DataError.NETWORK
        viewModel.effects.test {
            viewModel.onEvent(ProfileEvent.DeleteAccountClicked)
            viewModel.onEvent(ProfileEvent.ConfirmDeleteAccount)
            assertEquals(ProfileEffect.ShowMessage(Res.string.message_error_network), awaitItem())
        }
        assertEquals(0, cleaner.calls)
        assertEquals(false, assertIs<ProfileUiState.Content>(viewModel.uiState.value).isBusy)
    }

    @Test
    fun editDisplayNameSavesThroughUseCase() {
        val viewModel = viewModel()
        viewModel.onEvent(ProfileEvent.EditNameClicked)
        viewModel.onEvent(ProfileEvent.NameDraftChanged("Grace Hopper"))
        viewModel.onEvent(ProfileEvent.SaveName)

        val state = assertIs<ProfileUiState.Content>(viewModel.uiState.value)
        assertEquals("Grace Hopper", state.profile.displayName)
        assertEquals("GH", state.initials)
        assertEquals(ProfileDialog.None, state.dialog)
    }
}
