package com.example.myproject.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.settings.MarkReviewRequestedUseCase
import com.example.myproject.domain.settings.ObserveThemeModeUseCase
import com.example.myproject.domain.settings.SetThemeModeUseCase
import com.example.myproject.platform.AppConfig
import com.example.myproject.platform.Capability
import com.example.myproject.platform.has
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MS = 5_000L

class SettingsViewModel(
    private val appConfig: AppConfig,
    observeThemeMode: ObserveThemeModeUseCase,
    private val setThemeMode: SetThemeModeUseCase,
    private val markReviewRequested: MarkReviewRequestedUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeThemeMode()
        .map { mode ->
            SettingsUiState.Content(
                themeMode = mode,
                appVersion = appConfig.versionName,
                showAccount = appConfig.has(Capability.AUTH),
                showPremium = appConfig.has(Capability.BILLING),
                showDeveloperTools = appConfig.isDebug,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState.Loading)

    private val effectChannel = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ThemeModeSelected -> setThemeMode(event.mode)
            SettingsEvent.RateAppClicked -> viewModelScope.launch { effectChannel.send(SettingsEffect.RequestReview) }
            SettingsEvent.ReviewFlowFinished -> markReviewRequested()
        }
    }
}
