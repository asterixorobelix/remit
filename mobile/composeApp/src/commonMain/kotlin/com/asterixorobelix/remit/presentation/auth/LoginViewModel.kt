package com.asterixorobelix.remit.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.auth.LoginUseCase
import com.asterixorobelix.remit.domain.auth.ValidateLoginUseCase
import com.asterixorobelix.remit.domain.common.toDataError
import com.asterixorobelix.remit.presentation.common.toMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

data class LoginForm(val email: String = "", val password: String = "")

sealed interface LoginUiState {
    val form: LoginForm

    data class Content(override val form: LoginForm = LoginForm()) : LoginUiState
    data class Loading(override val form: LoginForm) : LoginUiState
    data class Error(override val form: LoginForm, val message: StringResource) : LoginUiState
}

sealed interface LoginEvent {
    data class EmailChanged(val value: String) : LoginEvent
    data class PasswordChanged(val value: String) : LoginEvent
    data object Submit : LoginEvent
}

sealed interface LoginEffect {
    data object SignedIn : LoginEffect
}

class LoginViewModel(
    private val validateLogin: ValidateLoginUseCase,
    private val login: LoginUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow<LoginUiState>(LoginUiState.Content())
    val uiState: StateFlow<LoginUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> editForm { it.copy(email = event.value) }
            is LoginEvent.PasswordChanged -> editForm { it.copy(password = event.value) }
            LoginEvent.Submit -> submit()
        }
    }

    private fun editForm(transform: (LoginForm) -> LoginForm) {
        val current = mutableState.value
        if (current is LoginUiState.Loading) return
        mutableState.value = LoginUiState.Content(transform(current.form))
    }

    private fun submit() {
        val current = mutableState.value
        if (current is LoginUiState.Loading) return
        val form = current.form
        val invalid = validateLogin(form.email, form.password)
        if (invalid != null) {
            mutableState.value = LoginUiState.Error(form, invalid.toMessage())
            return
        }
        mutableState.value = LoginUiState.Loading(form)
        viewModelScope.launch {
            login(form.email, form.password)
                .onSuccess {
                    mutableState.value = LoginUiState.Content(LoginForm())
                    effectChannel.send(LoginEffect.SignedIn)
                }
                .onFailure { error ->
                    mutableState.value = LoginUiState.Error(form, error.toDataError().toMessage())
                }
        }
    }
}
