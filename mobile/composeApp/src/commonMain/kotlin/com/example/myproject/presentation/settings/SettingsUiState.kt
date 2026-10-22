package com.example.myproject.presentation.settings

import com.example.myproject.domain.settings.ThemeMode

sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Content(
        val themeMode: ThemeMode,
        val appVersion: String,
        /** Profile/account entry; only when the `auth` capability is enabled. */
        val showAccount: Boolean,
        /** Premium entry; only when the `billing` capability is enabled. */
        val showPremium: Boolean,
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
