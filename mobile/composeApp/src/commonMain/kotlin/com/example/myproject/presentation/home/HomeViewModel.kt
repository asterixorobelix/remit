package com.example.myproject.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.home.DayPeriod
import com.example.myproject.domain.home.GetDayPeriodUseCase
import com.example.myproject.domain.session.GetCachedProfileUseCase
import com.example.myproject.domain.task.ObserveRecentTasksUseCase
import com.example.myproject.domain.task.Task
import com.example.myproject.platform.AppConfig
import com.example.myproject.platform.Capability
import com.example.myproject.platform.has
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Destinations Home can open. Each maps to a route in AppNavigation. */
enum class HomeAction { TASKS, SEARCH, PROFILE, SETTINGS, MAP, PREMIUM }

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val displayName: String,
        val dayPeriod: DayPeriod,
        val featuredAction: HomeAction,
        val quickActions: List<HomeAction>,
        /** Latest tasks, or null when the Tasks (`offline`) capability is disabled. */
        val recentTasks: List<Task>?,
    ) : HomeUiState
}

private const val STOP_TIMEOUT_MS = 5_000L

class HomeViewModel(
    appConfig: AppConfig,
    getCachedProfile: GetCachedProfileUseCase,
    getDayPeriod: GetDayPeriodUseCase,
    observeRecentTasks: ObserveRecentTasksUseCase?,
) : ViewModel() {

    private val quickActions: List<HomeAction> = buildList {
        if (appConfig.has(Capability.OFFLINE)) add(HomeAction.TASKS)
        add(HomeAction.SEARCH)
        if (appConfig.has(Capability.MAPS)) add(HomeAction.MAP)
        if (appConfig.has(Capability.AUTH)) add(HomeAction.PROFILE)
        if (appConfig.has(Capability.BILLING)) add(HomeAction.PREMIUM)
        add(HomeAction.SETTINGS)
    }

    val uiState: StateFlow<HomeUiState> = (observeRecentTasks?.invoke() ?: flowOf(null))
        .catch { emit(emptyList()) }
        .map { recent ->
            HomeUiState.Content(
                displayName = getCachedProfile()?.displayName.orEmpty(),
                dayPeriod = getDayPeriod(),
                featuredAction = quickActions.first(),
                quickActions = quickActions,
                recentTasks = recent,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)
}
