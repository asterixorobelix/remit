package com.asterixorobelix.remit.presentation.payments.history

import com.asterixorobelix.remit.shared.domain.model.Payment

sealed interface PaymentHistoryUiState {
    data object Loading : PaymentHistoryUiState

    data object Empty : PaymentHistoryUiState

    /** Newest first; Firestore pushes changes, so the list updates without a refresh. */
    data class Content(val payments: List<Payment>, val isRefreshing: Boolean = false) : PaymentHistoryUiState

    data object Error : PaymentHistoryUiState
}

sealed interface PaymentHistoryEvent {
    /** Reconnects the live listener: after an error, or by pulling to refresh. */
    data object Refresh : PaymentHistoryEvent
}
