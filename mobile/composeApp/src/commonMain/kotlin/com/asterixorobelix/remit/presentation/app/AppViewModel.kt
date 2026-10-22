package com.asterixorobelix.remit.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.session.ObserveSessionExpiredUseCase
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
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AppUiState {
    /** Resolving the start destination (reads secure storage off the main thread). */
    data object Loading : AppUiState

    data class Ready(
        /** Computed once per process so recompositions never move the start of the graph. */
        val startDestination: StartDestination,
        val updateRequired: Boolean = false,
    ) : AppUiState
}

sealed interface AppEffect {
    /** The server revoked the session; show sign-in. */
    data object SessionExpired : AppEffect
    data object RequestReview : AppEffect
}

/** Launch-time use cases of [AppViewModel], grouped to keep its constructor small. */
class AppLaunchUseCases(
    val resolveStartDestination: ResolveStartDestinationUseCase,
    val recordAppOpen: RecordAppOpenUseCase,
    val markReviewRequested: MarkReviewRequestedUseCase,
    val observeSessionExpired: ObserveSessionExpiredUseCase,
)

/** Root ViewModel: start destination, theme, force-update gate, review prompt, session expiry. */
class AppViewModel(
    appConfig: AppConfig,
    observeThemeMode: ObserveThemeModeUseCase,
    private val launch: AppLaunchUseCases,
    checkForceUpdate: CheckForceUpdateUseCase?,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = observeThemeMode()

    /** Destinations available for this build's capabilities. */
    val navigationFeatures: NavigationFeatures = NavigationFeatures.from(appConfig)

    /** Release package name used for the store listing. */
    val applicationId: String = appConfig.applicationId

    private val authEnabled = navigationFeatures.auth
    private val startDestination = MutableStateFlow<StartDestination?>(null)
    private val updateRequired = MutableStateFlow(false)

    val uiState: StateFlow<AppUiState> = combine(startDestination, updateRequired) { start, mustUpdate ->
        if (start == null) AppUiState.Loading else AppUiState.Ready(start, mustUpdate)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppUiState.Loading)

    private val effectChannel = Channel<AppEffect>(Channel.BUFFERED)
    val effects: Flow<AppEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { startDestination.value = launch.resolveStartDestination(authEnabled) }
        viewModelScope.launch {
            if (launch.recordAppOpen()) effectChannel.send(AppEffect.RequestReview)
        }
        if (checkForceUpdate != null) {
            viewModelScope.launch { updateRequired.value = checkForceUpdate.invoke(appConfig.versionName) }
        }
        if (authEnabled) {
            viewModelScope.launch {
                launch.observeSessionExpired().collect { effectChannel.send(AppEffect.SessionExpired) }
            }
        }
    }

    fun onReviewFlowFinished() = launch.markReviewRequested()
}
