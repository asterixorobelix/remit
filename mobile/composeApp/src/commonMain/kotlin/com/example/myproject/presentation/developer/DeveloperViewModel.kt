package com.example.myproject.presentation.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.developer.GetApiBaseUrlOverrideUseCase
import com.example.myproject.domain.developer.ResetDevelopmentStateUseCase
import com.example.myproject.domain.developer.SetApiBaseUrlOverrideUseCase
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.developer_invalid_url
import com.example.myproject.generated.resources.developer_saved
import com.example.myproject.platform.AppConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

sealed interface DeveloperUiState {
    data class Content(
        val environment: String,
        val versionName: String,
        val capabilities: String,
        val defaultApiUrl: String,
        val apiUrlDraft: String,
        val urlError: StringResource? = null,
    ) : DeveloperUiState
}

sealed interface DeveloperEvent {
    data class ApiUrlChanged(val value: String) : DeveloperEvent
    data object SaveApiUrl : DeveloperEvent
    data object ResetApp : DeveloperEvent
}

sealed interface DeveloperEffect {
    data class ShowMessage(val message: StringResource) : DeveloperEffect

    /** Local state was wiped; restart the navigation graph at onboarding. */
    data object AppReset : DeveloperEffect
}

class DeveloperViewModel(
    appConfig: AppConfig,
    getApiBaseUrlOverride: GetApiBaseUrlOverrideUseCase,
    private val setApiBaseUrlOverride: SetApiBaseUrlOverrideUseCase,
    private val resetDevelopmentState: ResetDevelopmentStateUseCase,
) : ViewModel() {

    private val mutableState = MutableStateFlow(
        DeveloperUiState.Content(
            environment = appConfig.environment.name,
            versionName = appConfig.versionName,
            capabilities = appConfig.capabilities.joinToString { it.name.lowercase() },
            defaultApiUrl = appConfig.apiBaseUrl,
            apiUrlDraft = getApiBaseUrlOverride() ?: appConfig.apiBaseUrl,
        ),
    )
    val uiState: StateFlow<DeveloperUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<DeveloperEffect>(Channel.BUFFERED)
    val effects: Flow<DeveloperEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: DeveloperEvent) {
        when (event) {
            is DeveloperEvent.ApiUrlChanged -> mutableState.update {
                it.copy(apiUrlDraft = event.value, urlError = null)
            }

            DeveloperEvent.SaveApiUrl -> saveApiUrl()

            DeveloperEvent.ResetApp -> viewModelScope.launch {
                resetDevelopmentState()
                effectChannel.send(DeveloperEffect.AppReset)
            }
        }
    }

    private fun saveApiUrl() {
        val current = mutableState.value
        // Saving the build's own URL removes the override instead of pinning it.
        val draft = current.apiUrlDraft.takeUnless { it.trim() == current.defaultApiUrl }.orEmpty()
        if (setApiBaseUrlOverride(draft)) {
            viewModelScope.launch { effectChannel.send(DeveloperEffect.ShowMessage(Res.string.developer_saved)) }
        } else {
            mutableState.value = current.copy(urlError = Res.string.developer_invalid_url)
        }
    }
}
