package com.example.myproject.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.auth_email_label
import com.example.myproject.generated.resources.auth_no_account_register
import com.example.myproject.generated.resources.auth_password_label
import com.example.myproject.generated.resources.auth_sign_in
import com.example.myproject.generated.resources.auth_sign_in_title
import com.example.myproject.ui.components.AppTextButton
import com.example.myproject.ui.components.PrimaryButton
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                LoginEffect.SignedIn -> currentOnLoginSuccess()
            }
        }
    }

    LoginContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToRegister = onNavigateToRegister,
    )
}

/** Stateless login form; internal so the desktop screenshot tests can render it with fake state. */
@Composable
internal fun LoginContent(
    uiState: LoginUiState,
    onEvent: (LoginEvent) -> Unit,
    onNavigateToRegister: () -> Unit,
) {
    val isLoading = uiState is LoginUiState.Loading
    val error = (uiState as? LoginUiState.Error)?.message

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.auth_sign_in_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

        AuthTextField(
            value = uiState.form.email,
            onValueChange = { onEvent(LoginEvent.EmailChanged(it)) },
            label = Res.string.auth_email_label,
            isError = error != null,
            enabled = !isLoading,
            keyboardType = KeyboardType.Email,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        PasswordField(
            value = uiState.form.password,
            onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
            label = Res.string.auth_password_label,
            isError = error != null,
            enabled = !isLoading,
        )

        AuthErrorText(error)

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))

        PrimaryButton(
            onClick = { onEvent(LoginEvent.Submit) },
            text = stringResource(Res.string.auth_sign_in),
            modifier = Modifier.fillMaxWidth(),
            loading = isLoading,
            enabled = !isLoading,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        AppTextButton(
            onClick = onNavigateToRegister,
            text = stringResource(Res.string.auth_no_account_register),
        )
    }
}
