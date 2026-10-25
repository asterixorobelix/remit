package com.asterixorobelix.remit.presentation.payments.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.CrashLogger
import com.asterixorobelix.remit.shared.domain.repository.HistoryFailure
import com.asterixorobelix.remit.shared.domain.repository.PaymentHistoryException
import com.asterixorobelix.remit.shared.domain.usecase.ObservePaymentHistoryUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/**
 * How long the listener outlives the screen's last subscriber: long enough to ride out a
 * configuration change, short enough that a backgrounded app stops reading from Firestore.
 */
internal const val HISTORY_STOP_TIMEOUT_MS = 5_000L

/**
 * Listens to the user's payments while the screen is on display. The Firestore listener starts
 * with the first subscriber and is removed [HISTORY_STOP_TIMEOUT_MS] after the last one leaves (the
 * app went to the background, or another tab is showing); coming back reconnects it and keeps the
 * last list on screen until the listener answers.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentHistoryViewModel(
    private val observeHistory: ObservePaymentHistoryUseCase,
    private val crashLogger: CrashLogger,
    private val appConfig: AppConfig,
) : ViewModel() {
    private val refreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val uiState: StateFlow<PaymentHistoryUiState> = merge(flowOf(false), refreshes.map { true })
        .flatMapLatest { isRefresh -> history(isRefresh) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(HISTORY_STOP_TIMEOUT_MS),
            initialValue = PaymentHistoryUiState.Loading,
        )

    fun onEvent(event: PaymentHistoryEvent) {
        when (event) {
            PaymentHistoryEvent.Refresh -> refreshes.tryEmit(Unit)
        }
    }

    private fun history(isRefresh: Boolean): Flow<PaymentHistoryUiState> = observeHistory()
        .map { payments ->
            if (payments.isEmpty()) PaymentHistoryUiState.Empty else PaymentHistoryUiState.Content(payments)
        }
        .onStart { if (isRefresh) emit(refreshing()) }
        .catch { error ->
            crashLogger.logException(error, "Payment history listener failed")
            val reason = (error as? PaymentHistoryException)?.reason ?: HistoryFailure.UNAVAILABLE
            emit(PaymentHistoryUiState.Error(reason = reason, setupHelp = setupHelp(reason)))
        }

    private fun setupHelp(reason: HistoryFailure): SetupHelp? = when {
        !appConfig.isDebug -> null
        reason == HistoryFailure.NOT_SET_UP -> SetupHelp.ConnectFirebase
        reason == HistoryFailure.NETWORK -> appConfig.firebaseEmulatorHost?.let(SetupHelp::StartEmulators)
        else -> null
    }

    /** A refresh keeps the list on screen with the indicator showing; after an error it shows Loading. */
    private fun refreshing(): PaymentHistoryUiState =
        (uiState.value as? PaymentHistoryUiState.Content)?.copy(isRefreshing = true)
            ?: PaymentHistoryUiState.Loading
}
