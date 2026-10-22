package com.asterixorobelix.remit.presentation.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_navigate_back
import com.asterixorobelix.remit.generated.resources.developer_api_url
import com.asterixorobelix.remit.generated.resources.developer_api_url_note
import com.asterixorobelix.remit.generated.resources.developer_environment
import com.asterixorobelix.remit.generated.resources.developer_reset
import com.asterixorobelix.remit.generated.resources.developer_save
import com.asterixorobelix.remit.generated.resources.developer_title
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onAppReset: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeveloperViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnAppReset by rememberUpdatedState(onAppReset)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DeveloperEffect.ShowMessage -> snackbarHostState.showSnackbar(getString(effect.message))
                DeveloperEffect.AppReset -> currentOnAppReset()
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.developer_title)) },
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
            is DeveloperUiState.Content -> DeveloperContent(state, viewModel::onEvent, Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun DeveloperContent(
    state: DeveloperUiState.Content,
    onEvent: (DeveloperEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
    ) {
        Text(
            text = stringResource(
                Res.string.developer_environment,
                state.environment,
                state.versionName,
                state.capabilities,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = state.apiUrlDraft,
            onValueChange = { onEvent(DeveloperEvent.ApiUrlChanged(it)) },
            label = { Text(stringResource(Res.string.developer_api_url)) },
            isError = state.urlError != null,
            supportingText = {
                Text(stringResource(state.urlError ?: Res.string.developer_api_url_note))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onEvent(DeveloperEvent.SaveApiUrl) }) {
            Text(stringResource(Res.string.developer_save))
        }
        OutlinedButton(onClick = { onEvent(DeveloperEvent.ResetApp) }) {
            Text(stringResource(Res.string.developer_reset))
        }
    }
}
