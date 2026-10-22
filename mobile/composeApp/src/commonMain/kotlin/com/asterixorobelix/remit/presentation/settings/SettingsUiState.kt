package com.asterixorobelix.remit.presentation.settings

import com.asterixorobelix.remit.domain.settings.ThemeMode

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val themeMode: ThemeMode,
        val appVersion: String,
        /** Debug builds only. */
        val showDeveloperTools: Boolean,
    ) : SettingsUiState
}

sealed interface SettingsEvent {
    data class ThemeModeSelected(val mode: ThemeMode) : SettingsEvent
    data object RateAppClicked : SettingsEvent
    data object ReviewFlowFinished : SettingsEvent
}

sealed interface SettingsEffect {
    data object RequestReview : SettingsEffect
}
