package com.asterixorobelix.remit.presentation.feedback

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_navigate_back
import com.asterixorobelix.remit.generated.resources.feedback_description
import com.asterixorobelix.remit.generated.resources.feedback_message_error
import com.asterixorobelix.remit.generated.resources.feedback_message_hint
import com.asterixorobelix.remit.generated.resources.feedback_no_email_app
import com.asterixorobelix.remit.generated.resources.feedback_subject_error
import com.asterixorobelix.remit.generated.resources.feedback_subject_hint
import com.asterixorobelix.remit.generated.resources.feedback_submit
import com.asterixorobelix.remit.generated.resources.settings_feedback
import com.asterixorobelix.remit.generated.resources.support_email
import com.asterixorobelix.remit.platform.openUrl
import com.asterixorobelix.remit.presentation.common.CollectEffects
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.preview.ScreenStates
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val MESSAGE_MIN_LINES = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedbackViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val supportEmail = stringResource(Res.string.support_email)
    val currentOnNavigateBack by rememberUpdatedState(onNavigateBack)

    CollectEffects(viewModel.effects) { effect ->
        when (effect) {
            is FeedbackEffect.ComposeEmail -> if (openUrl(effect.mailtoUrl)) {
                currentOnNavigateBack()
            } else {
                snackbarHostState.showSnackbar(getString(Res.string.feedback_no_email_app, supportEmail))
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_feedback)) },
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
        when (val state = uiState) {
            is FeedbackUiState.Content -> FeedbackContent(
                state = state,
                onEvent = viewModel::onEvent,
                onSubmit = { viewModel.onEvent(FeedbackEvent.Submit(supportEmail)) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun FeedbackContent(
    state: FeedbackUiState.Content,
    onEvent: (FeedbackEvent) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontalPadding),
    ) {
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        Text(
            text = stringResource(Res.string.feedback_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        OutlinedTextField(
            value = state.subject,
            onValueChange = { onEvent(FeedbackEvent.SubjectChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(Res.string.feedback_subject_hint)) },
            isError = state.subjectError,
            supportingText = if (state.subjectError) {
                { Text(text = stringResource(Res.string.feedback_subject_error)) }
            } else {
                null
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingMedium))
        OutlinedTextField(
            value = state.message,
            onValueChange = { onEvent(FeedbackEvent.MessageChanged(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text = stringResource(Res.string.feedback_message_hint)) },
            isError = state.messageError,
            supportingText = if (state.messageError) {
                { Text(text = stringResource(Res.string.feedback_message_error)) }
            } else {
                null
            },
            minLines = MESSAGE_MIN_LINES,
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
        PrimaryButton(
            onClick = onSubmit,
            text = stringResource(Res.string.feedback_submit),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSection))
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun FeedbackContentPreview() {
    val states = listOf(
        FeedbackUiState.Content(),
        FeedbackUiState.Content(subjectError = true, messageError = true),
        FeedbackUiState.Content(
            subject = "History shows the wrong time",
            message = "A payment I sent at 10:30 shows 08:30 in History.",
        ),
    )
    ScreenStates(states) { state -> FeedbackContent(state = state, onEvent = {}, onSubmit = {}) }
}
