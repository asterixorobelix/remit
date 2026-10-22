package com.asterixorobelix.remit.presentation.profile

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.auth.DeleteAccountUseCase
import com.asterixorobelix.remit.domain.auth.LogoutUseCase
import com.asterixorobelix.remit.domain.auth.RefreshProfileUseCase
import com.asterixorobelix.remit.domain.auth.UpdateDisplayNameUseCase
import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.domain.session.ComputeInitialsUseCase
import com.asterixorobelix.remit.domain.session.GetCachedProfileUseCase
import com.asterixorobelix.remit.domain.session.UserProfile
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.message_error_network
import com.asterixorobelix.remit.testing.FakeAuthRepository
import com.asterixorobelix.remit.testing.FakeProfileCache
import com.asterixorobelix.remit.testing.RecordingCleaner
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
