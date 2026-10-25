package com.asterixorobelix.remit.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.settings.MarkReviewRequestedUseCase
import com.asterixorobelix.remit.domain.settings.ObserveThemeModeUseCase
import com.asterixorobelix.remit.domain.settings.RecordAppOpenUseCase
import com.asterixorobelix.remit.domain.settings.ResolveStartDestinationUseCase
import com.asterixorobelix.remit.domain.settings.StartDestination
import com.asterixorobelix.remit.domain.settings.ThemeMode
import com.asterixorobelix.remit.domain.update.CheckForceUpdateUseCase
import com.asterixorobelix.remit.navigation.NavigationFeatures
import com.asterixorobelix.remit.platform.AppConfig
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppUiState {
    /** Reading the start destination and theme; the splash screen stays up meanwhile. */
    data object Loading : AppUiState

    data class Ready(
        /** Computed once per process so recompositions never move the start of the graph. */
        val startDestination: StartDestination,
        val updateRequired: Boolean = false,
    ) : AppUiState
}

sealed interface AppEffect {
    data object RequestReview : AppEffect
}

/** Launch-time use cases of [AppViewModel], grouped to keep its constructor small. */
class AppLaunchUseCases(
    val resolveStartDestination: ResolveStartDestinationUseCase,
    val recordAppOpen: RecordAppOpenUseCase,
    val markReviewRequested: MarkReviewRequestedUseCase,
)

/** Root ViewModel: start destination, theme, force-update gate, review prompt. */
class AppViewModel(
    appConfig: AppConfig,
    observeThemeMode: ObserveThemeModeUseCase,
    private val launch: AppLaunchUseCases,
    checkForceUpdate: CheckForceUpdateUseCase?,
) : ViewModel() {

    /** The saved theme, null until it has been read. */
    private val savedTheme: StateFlow<ThemeMode?> =
        observeThemeMode().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode> =
        savedTheme.map { it ?: ThemeMode.SYSTEM }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Destinations available for this build's capabilities. */
    val navigationFeatures: NavigationFeatures = NavigationFeatures.from(appConfig)

    /** Release package name used for the store listing. */
    val applicationId: String = appConfig.applicationId

    private val startDestination = MutableStateFlow<StartDestination?>(null)
    private val updateRequired = MutableStateFlow(false)

    /** Ready once the start destination and the theme are known, so the first frame is final. */
    val uiState: StateFlow<AppUiState> =
        combine(startDestination, updateRequired, savedTheme) { start, mustUpdate, theme ->
            if (start == null || theme == null) AppUiState.Loading else AppUiState.Ready(start, mustUpdate)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState.Loading)

    private val effectChannel = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { startDestination.value = launch.resolveStartDestination() }
        viewModelScope.launch {
            if (launch.recordAppOpen()) effectChannel.send(AppEffect.RequestReview)
        }
        if (checkForceUpdate != null) {
            viewModelScope.launch { updateRequired.value = checkForceUpdate.invoke(appConfig.versionName) }
        }
    }

    fun onReviewFlowFinished() {
        viewModelScope.launch { launch.markReviewRequested() }
    }
}
