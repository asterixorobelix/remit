package com.example.myproject.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.auth.RegisterUseCase
import com.example.myproject.domain.auth.ValidateRegistrationUseCase
import com.example.myproject.domain.common.toDataError
import com.example.myproject.presentation.common.toMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

data class RegisterForm(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
)

sealed interface RegisterUiState {
    val form: RegisterForm

    data class Content(override val form: RegisterForm = RegisterForm()) : RegisterUiState
    data class Loading(override val form: RegisterForm) : RegisterUiState
    data class Error(override val form: RegisterForm, val message: StringResource) : RegisterUiState
}

sealed interface RegisterEvent {
    data class DisplayNameChanged(val value: String) : RegisterEvent
    data class EmailChanged(val value: String) : RegisterEvent
    data class PasswordChanged(val value: String) : RegisterEvent
    data class ConfirmPasswordChanged(val value: String) : RegisterEvent
    data object Submit : RegisterEvent
}

sealed interface RegisterEffect {
    data object Registered : RegisterEffect
}

class RegisterViewModel(
    private val validateRegistration: ValidateRegistrationUseCase,
    private val register: RegisterUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow<RegisterUiState>(RegisterUiState.Content())
    val uiState: StateFlow<RegisterUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<RegisterEffect>(Channel.BUFFERED)
    val effects: Flow<RegisterEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: RegisterEvent) {
        when (event) {
            is RegisterEvent.DisplayNameChanged -> editForm { it.copy(displayName = event.value) }
            is RegisterEvent.EmailChanged -> editForm { it.copy(email = event.value) }
            is RegisterEvent.PasswordChanged -> editForm { it.copy(password = event.value) }
            is RegisterEvent.ConfirmPasswordChanged -> editForm { it.copy(confirmPassword = event.value) }
            RegisterEvent.Submit -> submit()
        }
    }

    private fun editForm(transform: (RegisterForm) -> RegisterForm) {
        val current = mutableState.value
        if (current is RegisterUiState.Loading) return
        mutableState.value = RegisterUiState.Content(transform(current.form))
    }

    private fun submit() {
        val current = mutableState.value
        if (current is RegisterUiState.Loading) return
        val form = current.form
        val invalid = validateRegistration(form.displayName, form.email, form.password, form.confirmPassword)
        if (invalid != null) {
            mutableState.value = RegisterUiState.Error(form, invalid.toMessage())
            return
        }
        mutableState.value = RegisterUiState.Loading(form)
        viewModelScope.launch {
            register(form.displayName, form.email, form.password)
                .onSuccess {
                    mutableState.value = RegisterUiState.Content(RegisterForm())
                    effectChannel.send(RegisterEffect.Registered)
                }
                .onFailure { error ->
                    mutableState.value = RegisterUiState.Error(form, error.toDataError().toMessage())
                }
        }
    }
}
