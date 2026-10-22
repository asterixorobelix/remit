package com.example.myproject.presentation.auth

import app.cash.turbine.test
import com.example.myproject.domain.auth.RegisterUseCase
import com.example.myproject.domain.auth.ValidateRegistrationUseCase
import com.example.myproject.domain.common.DataError
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.auth_error_email_taken
import com.example.myproject.generated.resources.auth_error_passwords_do_not_match
import com.example.myproject.generated.resources.validation_password_short
import com.example.myproject.testing.FakeAuthRepository
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    private val repository = FakeAuthRepository()
    private lateinit var viewModel: RegisterViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = RegisterViewModel(ValidateRegistrationUseCase(), RegisterUseCase(repository))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fill(password: String = "password1", confirm: String = password) {
        viewModel.onEvent(RegisterEvent.DisplayNameChanged("Ada"))
        viewModel.onEvent(RegisterEvent.EmailChanged("ada@example.com"))
        viewModel.onEvent(RegisterEvent.PasswordChanged(password))
        viewModel.onEvent(RegisterEvent.ConfirmPasswordChanged(confirm))
    }

    @Test
    fun mismatchedPasswordsAreRejectedLocally() {
        fill(password = "password1", confirm = "password2")
        viewModel.onEvent(RegisterEvent.Submit)
        assertEquals(
            Res.string.auth_error_passwords_do_not_match,
            assertIs<RegisterUiState.Error>(viewModel.uiState.value).message,
        )
        assertNull(repository.lastEmail)
    }

    @Test
    fun shortPasswordMatchesBackendRule() {
        fill(password = "short")
        viewModel.onEvent(RegisterEvent.Submit)
        assertEquals(
            Res.string.validation_password_short,
            assertIs<RegisterUiState.Error>(viewModel.uiState.value).message,
        )
    }

    @Test
    fun successEmitsRegistered() = runTest {
        viewModel.effects.test {
            fill()
            viewModel.onEvent(RegisterEvent.Submit)
            assertEquals(RegisterEffect.Registered, awaitItem())
        }
        assertEquals("Ada", repository.lastDisplayName)
    }

    @Test
    fun duplicateEmailShowsTypedMessage() {
        repository.failure = DataError.EMAIL_TAKEN
        fill()
        viewModel.onEvent(RegisterEvent.Submit)
        assertEquals(
            Res.string.auth_error_email_taken,
            assertIs<RegisterUiState.Error>(viewModel.uiState.value).message,
        )
    }
}
