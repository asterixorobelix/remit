package com.asterixorobelix.remit.presentation.payments.history

import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.repository.HistoryFailure

sealed interface PaymentHistoryUiState {
    data object Loading : PaymentHistoryUiState

    data object Empty : PaymentHistoryUiState

    /** Newest first; Firestore pushes changes, so the list updates without a refresh. */
    data class Content(val payments: List<Payment>, val isRefreshing: Boolean = false) : PaymentHistoryUiState

    /** [setupHelp]: debug builds only, shown to the developer instead of the user-facing message. */
    data class Error(val reason: HistoryFailure, val setupHelp: SetupHelp? = null) : PaymentHistoryUiState
}

/** What a developer has to do locally for History to work in a debug build. */
sealed interface SetupHelp {
    /** The build has no Firebase: no google-services.json and a blank emulator host. */
    data object ConnectFirebase : SetupHelp

    /** The build uses the Firebase emulators at [host], and they did not answer. */
    data class StartEmulators(val host: String) : SetupHelp
}

sealed interface PaymentHistoryEvent {
    /** Reconnects the live listener: after an error, or by pulling to refresh. */
    data object Refresh : PaymentHistoryEvent
}
