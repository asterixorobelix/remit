package com.asterixorobelix.remit.presentation.auth

import app.cash.turbine.test
import com.asterixorobelix.remit.domain.auth.LoginUseCase
import com.asterixorobelix.remit.domain.auth.ValidateLoginUseCase
import com.asterixorobelix.remit.domain.common.DataError
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.auth_error_all_fields_required
import com.asterixorobelix.remit.generated.resources.auth_error_invalid_credentials
import com.asterixorobelix.remit.generated.resources.validation_email_invalid
import com.asterixorobelix.remit.testing.FakeAuthRepository
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
class LoginViewModelTest {
    private val repository = FakeAuthRepository()
    private lateinit var viewModel: LoginViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = LoginViewModel(ValidateLoginUseCase(), LoginUseCase(repository))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun fill(email: String, password: String) {
        viewModel.onEvent(LoginEvent.EmailChanged(email))
        viewModel.onEvent(LoginEvent.PasswordChanged(password))
    }

    @Test
    fun startsWithEmptyForm() {
        assertEquals(LoginUiState.Content(LoginForm()), viewModel.uiState.value)
    }

    @Test
    fun emptyFieldsShowValidationErrorWithoutCallingServer() {
        viewModel.onEvent(LoginEvent.Submit)
        val state = assertIs<LoginUiState.Error>(viewModel.uiState.value)
        assertEquals(Res.string.auth_error_all_fields_required, state.message)
        assertNull(repository.lastEmail)
    }

    @Test
    fun malformedEmailIsRejected() {
        fill("nope", "password1")
        viewModel.onEvent(LoginEvent.Submit)
        assertEquals(Res.string.validation_email_invalid, assertIs<LoginUiState.Error>(viewModel.uiState.value).message)
    }

    @Test
    fun typingClearsError() {
        viewModel.onEvent(LoginEvent.Submit)
        viewModel.onEvent(LoginEvent.EmailChanged("a"))
        assertIs<LoginUiState.Content>(viewModel.uiState.value)
    }

    @Test
    fun successEmitsSignedInAndNormalizesEmail() = runTest {
        viewModel.effects.test {
            fill(" Test@Example.COM ", "password1")
            viewModel.onEvent(LoginEvent.Submit)
            assertEquals(LoginEffect.SignedIn, awaitItem())
        }
        assertEquals("test@example.com", repository.lastEmail)
        assertEquals(LoginForm(), viewModel.uiState.value.form)
    }

    @Test
    fun invalidCredentialsMapToTypedMessage() {
        repository.failure = DataError.INVALID_CREDENTIALS
        fill("test@example.com", "wrong-password")
        viewModel.onEvent(LoginEvent.Submit)
        val state = assertIs<LoginUiState.Error>(viewModel.uiState.value)
        assertEquals(Res.string.auth_error_invalid_credentials, state.message)
        assertEquals("test@example.com", state.form.email)
    }
}
