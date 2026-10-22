package com.asterixorobelix.remit.presentation.auth

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
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.auth_confirm_password_label
import com.asterixorobelix.remit.generated.resources.auth_create_account
import com.asterixorobelix.remit.generated.resources.auth_display_name_label
import com.asterixorobelix.remit.generated.resources.auth_email_label
import com.asterixorobelix.remit.generated.resources.auth_has_account_sign_in
import com.asterixorobelix.remit.generated.resources.auth_password_label
import com.asterixorobelix.remit.generated.resources.auth_register_title
import com.asterixorobelix.remit.ui.components.AppTextButton
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnRegisterSuccess by rememberUpdatedState(onRegisterSuccess)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                RegisterEffect.Registered -> currentOnRegisterSuccess()
            }
        }
    }

    RegisterContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToLogin = onNavigateToLogin,
    )
}

@Composable
private fun RegisterContent(
    uiState: RegisterUiState,
    onEvent: (RegisterEvent) -> Unit,
    onNavigateToLogin: () -> Unit,
) {
    val isLoading = uiState is RegisterUiState.Loading
    val error = (uiState as? RegisterUiState.Error)?.message
    val form = uiState.form

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.auth_register_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

        AuthTextField(
            value = form.displayName,
            onValueChange = { onEvent(RegisterEvent.DisplayNameChanged(it)) },
            label = Res.string.auth_display_name_label,
            isError = error != null,
            enabled = !isLoading,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        AuthTextField(
            value = form.email,
            onValueChange = { onEvent(RegisterEvent.EmailChanged(it)) },
            label = Res.string.auth_email_label,
            isError = error != null,
            enabled = !isLoading,
            keyboardType = KeyboardType.Email,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        PasswordField(
            value = form.password,
            onValueChange = { onEvent(RegisterEvent.PasswordChanged(it)) },
            label = Res.string.auth_password_label,
            isError = error != null,
            enabled = !isLoading,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        PasswordField(
            value = form.confirmPassword,
            onValueChange = { onEvent(RegisterEvent.ConfirmPasswordChanged(it)) },
            label = Res.string.auth_confirm_password_label,
            isError = error != null,
            enabled = !isLoading,
        )

        AuthErrorText(error)

        Spacer(modifier = Modifier.height(Dimens.spacingLarge))

        PrimaryButton(
            onClick = { onEvent(RegisterEvent.Submit) },
            text = stringResource(Res.string.auth_create_account),
            modifier = Modifier.fillMaxWidth(),
            loading = isLoading,
            enabled = !isLoading,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        AppTextButton(
            onClick = onNavigateToLogin,
            text = stringResource(Res.string.auth_has_account_sign_in),
        )
    }
}
