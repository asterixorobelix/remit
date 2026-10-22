package com.example.myproject.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.common.toDataError
import com.example.myproject.domain.notifications.AppNotification
import com.example.myproject.domain.notifications.MarkNotificationReadUseCase
import com.example.myproject.domain.notifications.ObserveNotificationsUseCase
import com.example.myproject.domain.notifications.RefreshNotificationsUseCase
import com.example.myproject.presentation.common.toMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

sealed interface NotificationsUiState {
    data object Loading : NotificationsUiState
    data class Content(
        val notifications: List<AppNotification>,
        val isRefreshing: Boolean = false,
    ) : NotificationsUiState
    data object Empty : NotificationsUiState
    data class Error(val message: StringResource) : NotificationsUiState
}

class NotificationsViewModel(
    observeNotifications: ObserveNotificationsUseCase,
    private val refreshNotifications: RefreshNotificationsUseCase,
    private val markNotificationRead: MarkNotificationReadUseCase,
) : ViewModel() {

    private val refreshing = MutableStateFlow(false)
    private val mutableState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeNotifications(), refreshing) { notifications, isRefreshing ->
                if (notifications.isEmpty()) {
                    NotificationsUiState.Empty
                } else {
                    NotificationsUiState.Content(notifications, isRefreshing)
                }
            }.collect { mutableState.value = it }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            refreshNotifications().onFailure { error ->
                if (mutableState.value !is NotificationsUiState.Content) {
                    mutableState.value = NotificationsUiState.Error(error.toDataError().toMessage())
                }
            }
            refreshing.value = false
        }
    }

    fun markAsRead(id: String) {
        viewModelScope.launch { markNotificationRead(id) }
    }
}
