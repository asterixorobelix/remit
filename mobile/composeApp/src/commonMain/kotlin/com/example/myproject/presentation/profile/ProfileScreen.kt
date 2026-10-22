package com.example.myproject.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.button_cancel
import com.example.myproject.generated.resources.button_save
import com.example.myproject.generated.resources.cd_navigate_back
import com.example.myproject.generated.resources.profile_delete_account
import com.example.myproject.generated.resources.profile_delete_confirm
import com.example.myproject.generated.resources.profile_delete_message
import com.example.myproject.generated.resources.profile_delete_title
import com.example.myproject.generated.resources.profile_display_name_label
import com.example.myproject.generated.resources.profile_edit
import com.example.myproject.generated.resources.profile_sign_out
import com.example.myproject.generated.resources.profile_title
import com.example.myproject.ui.components.ConfirmationDialog
import com.example.myproject.ui.components.DestructiveButton
import com.example.myproject.ui.components.FullScreenError
import com.example.myproject.ui.components.SecondaryButton
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onSignedOut: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnSignedOut by rememberUpdatedState(onSignedOut)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ProfileEffect.SignedOut -> currentOnSignedOut()
                is ProfileEffect.ShowMessage -> snackbarHostState.showSnackbar(getString(effect.message))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.profile_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.cd_navigate_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)
        when (val state = uiState) {
            ProfileUiState.Loading -> Box(contentModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            is ProfileUiState.Error -> Column(contentModifier.fillMaxSize()) {
                FullScreenError(
                    message = stringResource(state.message),
                    onRetry = { viewModel.onEvent(ProfileEvent.Retry) },
                    modifier = Modifier.weight(1f),
                )
                DestructiveButton(
                    onClick = { viewModel.onEvent(ProfileEvent.SignOut) },
                    text = stringResource(Res.string.profile_sign_out),
                    modifier = Modifier.fillMaxWidth().padding(Dimens.screenHorizontalPadding),
                )
            }

            is ProfileUiState.Content -> {
                ProfileContent(state = state, onEvent = viewModel::onEvent, modifier = contentModifier)
                ProfileDialogs(dialog = state.dialog, onEvent = viewModel::onEvent)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState.Content,
    onEvent: (ProfileEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))
        Box(
            modifier = Modifier
                .size(Dimens.avatarSizeProfile)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = state.initials,
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
        Text(
            text = state.profile.displayName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        Text(
            text = state.profile.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))
        SecondaryButton(
            onClick = { onEvent(ProfileEvent.EditNameClicked) },
            text = stringResource(Res.string.profile_edit),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isBusy,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        DestructiveButton(
            onClick = { onEvent(ProfileEvent.SignOut) },
            text = stringResource(Res.string.profile_sign_out),
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isBusy,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSection))
        TextButton(
            onClick = { onEvent(ProfileEvent.DeleteAccountClicked) },
            enabled = !state.isBusy,
        ) {
            Text(
                text = stringResource(Res.string.profile_delete_account),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.isBusy) {
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ProfileDialogs(dialog: ProfileDialog, onEvent: (ProfileEvent) -> Unit) {
    when (dialog) {
        ProfileDialog.None -> Unit

        ProfileDialog.ConfirmDelete -> ConfirmationDialog(
            title = stringResource(Res.string.profile_delete_title),
            message = stringResource(Res.string.profile_delete_message),
            confirmLabel = stringResource(Res.string.profile_delete_confirm),
            onConfirm = { onEvent(ProfileEvent.ConfirmDeleteAccount) },
            onDismiss = { onEvent(ProfileEvent.DismissDialog) },
        )

        is ProfileDialog.EditName -> AlertDialog(
            onDismissRequest = { onEvent(ProfileEvent.DismissDialog) },
            title = { Text(stringResource(Res.string.profile_edit)) },
            text = {
                OutlinedTextField(
                    value = dialog.draft,
                    onValueChange = { onEvent(ProfileEvent.NameDraftChanged(it)) },
                    label = { Text(stringResource(Res.string.profile_display_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(ProfileEvent.SaveName) },
                    enabled = dialog.draft.isNotBlank(),
                ) { Text(stringResource(Res.string.button_save)) }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProfileEvent.DismissDialog) }) {
                    Text(stringResource(Res.string.button_cancel))
                }
            },
        )
    }
}
