package com.example.myproject.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.auth.DeleteAccountUseCase
import com.example.myproject.domain.auth.LogoutUseCase
import com.example.myproject.domain.auth.RefreshProfileUseCase
import com.example.myproject.domain.auth.UpdateDisplayNameUseCase
import com.example.myproject.domain.common.toDataError
import com.example.myproject.domain.session.ComputeInitialsUseCase
import com.example.myproject.domain.session.GetCachedProfileUseCase
import com.example.myproject.domain.session.UserProfile
import com.example.myproject.presentation.common.toMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

sealed interface ProfileDialog {
    data object None : ProfileDialog
    data class EditName(val draft: String) : ProfileDialog
    data object ConfirmDelete : ProfileDialog
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState

    data class Content(
        val profile: UserProfile,
        val initials: String,
        val isBusy: Boolean = false,
        val dialog: ProfileDialog = ProfileDialog.None,
    ) : ProfileUiState

    data class Error(val message: StringResource) : ProfileUiState
}

sealed interface ProfileEvent {
    data object Retry : ProfileEvent
    data object EditNameClicked : ProfileEvent
    data class NameDraftChanged(val value: String) : ProfileEvent
    data object SaveName : ProfileEvent
    data object DeleteAccountClicked : ProfileEvent
    data object ConfirmDeleteAccount : ProfileEvent
    data object DismissDialog : ProfileEvent
    data object SignOut : ProfileEvent
}

sealed interface ProfileEffect {
    /** Session ended (sign-out or account deletion); navigate to sign-in. */
    data object SignedOut : ProfileEffect
    data class ShowMessage(val message: StringResource) : ProfileEffect
}

class ProfileViewModel(
    private val getCachedProfile: GetCachedProfileUseCase,
    private val refreshProfile: RefreshProfileUseCase,
    private val computeInitials: ComputeInitialsUseCase,
    private val updateDisplayName: UpdateDisplayNameUseCase,
    private val logout: LogoutUseCase,
    private val deleteAccount: DeleteAccountUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow<ProfileUiState>(
        getCachedProfile()?.let(::contentFor) ?: ProfileUiState.Loading,
    )
    val uiState: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects: Flow<ProfileEffect> = effectChannel.receiveAsFlow()

    init {
        load()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Retry -> {
                mutableState.value = ProfileUiState.Loading
                load()
            }

            ProfileEvent.EditNameClicked -> updateContent {
                it.copy(dialog = ProfileDialog.EditName(it.profile.displayName))
            }

            is ProfileEvent.NameDraftChanged -> updateContent { content ->
                if (content.dialog is ProfileDialog.EditName) {
                    content.copy(dialog = ProfileDialog.EditName(event.value))
                } else {
                    content
                }
            }

            ProfileEvent.SaveName -> saveName()

            ProfileEvent.DeleteAccountClicked -> updateContent { it.copy(dialog = ProfileDialog.ConfirmDelete) }

            ProfileEvent.ConfirmDeleteAccount -> confirmDelete()

            ProfileEvent.DismissDialog -> updateContent { it.copy(dialog = ProfileDialog.None) }

            ProfileEvent.SignOut -> signOut()
        }
    }

    /** Shows the cached profile immediately and refreshes it from the server. */
    private fun load() {
        viewModelScope.launch {
            refreshProfile()
                .onSuccess { profile ->
                    mutableState.value = (mutableState.value as? ProfileUiState.Content)
                        ?.copy(profile = profile, initials = computeInitials(profile))
                        ?: contentFor(profile)
                }
                .onFailure { error ->
                    if (mutableState.value !is ProfileUiState.Content) {
                        mutableState.value = ProfileUiState.Error(error.toDataError().toMessage())
                    }
                }
        }
    }

    private fun saveName() {
        val content = mutableState.value as? ProfileUiState.Content ?: return
        val dialog = content.dialog as? ProfileDialog.EditName ?: return
        mutableState.value = content.copy(isBusy = true)
        viewModelScope.launch {
            updateDisplayName(dialog.draft)
                .onSuccess { profile -> mutableState.value = contentFor(profile) }
                .onFailure { error ->
                    updateContent { it.copy(isBusy = false) }
                    effectChannel.send(ProfileEffect.ShowMessage(error.toDataError().toMessage()))
                }
        }
    }

    private fun confirmDelete() {
        updateContent { it.copy(isBusy = true, dialog = ProfileDialog.None) }
        viewModelScope.launch {
            deleteAccount()
                .onSuccess { effectChannel.send(ProfileEffect.SignedOut) }
                .onFailure { error ->
                    updateContent { it.copy(isBusy = false) }
                    effectChannel.send(ProfileEffect.ShowMessage(error.toDataError().toMessage()))
                }
        }
    }

    private fun signOut() {
        updateContent { it.copy(isBusy = true, dialog = ProfileDialog.None) }
        viewModelScope.launch {
            logout()
            effectChannel.send(ProfileEffect.SignedOut)
        }
    }

    private fun updateContent(transform: (ProfileUiState.Content) -> ProfileUiState.Content) {
        val content = mutableState.value as? ProfileUiState.Content ?: return
        mutableState.value = transform(content)
    }

    private fun contentFor(profile: UserProfile) =
        ProfileUiState.Content(profile = profile, initials = computeInitials(profile))
}
