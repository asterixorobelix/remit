package com.asterixorobelix.remit.presentation.payments.history

import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.repository.HistoryFailure

sealed interface PaymentHistoryUiState {
    data object Loading : PaymentHistoryUiState

    data object Empty : PaymentHistoryUiState

    /** Newest first; Firestore pushes changes, so the list updates without a refresh. */
    data class Content(val payments: List<Payment>, val isRefreshing: Boolean = false) : PaymentHistoryUiState

    /**
     * [showSetupHelp]: a debug build without Firebase, so the screen tells the developer how to
     * connect it instead of showing the user-facing message.
     */
    data class Error(val reason: HistoryFailure, val showSetupHelp: Boolean = false) : PaymentHistoryUiState
}

sealed interface PaymentHistoryEvent {
    /** Reconnects the live listener: after an error, or by pulling to refresh. */
    data object Refresh : PaymentHistoryEvent
}
