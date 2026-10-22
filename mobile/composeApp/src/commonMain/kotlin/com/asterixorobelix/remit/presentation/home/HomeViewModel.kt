package com.asterixorobelix.remit.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.home.DayPeriod
import com.asterixorobelix.remit.domain.home.GetDayPeriodUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(val dayPeriod: DayPeriod) : HomeUiState
}

private const val STOP_TIMEOUT_MS = 5_000L

/** Placeholder Home tab: a greeting for the time of day. Replace its content with the app's main feature. */
class HomeViewModel(getDayPeriod: GetDayPeriodUseCase) : ViewModel() {

    // Recomputed each time the screen starts collecting, so the greeting follows the clock.
    val uiState: StateFlow<HomeUiState> = flow { emit(HomeUiState.Content(dayPeriod = getDayPeriod())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState.Loading)
}
