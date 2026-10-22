package com.asterixorobelix.remit.presentation.payments.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.platform.CrashLogger
import com.asterixorobelix.remit.shared.domain.usecase.ObservePaymentHistoryUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Listens to the user's payments for as long as the screen's ViewModel lives. */
class PaymentHistoryViewModel(
    private val observeHistory: ObservePaymentHistoryUseCase,
    private val crashLogger: CrashLogger,
) : ViewModel() {
    private val mutableState = MutableStateFlow<PaymentHistoryUiState>(PaymentHistoryUiState.Loading)
    val uiState: StateFlow<PaymentHistoryUiState> = mutableState.asStateFlow()

    private var listener: Job? = null

    init {
        listen()
    }

    fun onEvent(event: PaymentHistoryEvent) {
        when (event) {
            PaymentHistoryEvent.Refresh -> {
                mutableState.update { state ->
                    if (state is PaymentHistoryUiState.Content) {
                        state.copy(
                            isRefreshing = true,
                        )
                    } else {
                        PaymentHistoryUiState.Loading
                    }
                }
                listen()
            }
        }
    }

    private fun listen() {
        listener?.cancel()
        listener = viewModelScope.launch {
            observeHistory()
                .catch { error ->
                    crashLogger.logException(error, "Payment history listener failed")
                    mutableState.value = PaymentHistoryUiState.Error
                }
                .collect { payments ->
                    mutableState.value = if (payments.isEmpty()) {
                        PaymentHistoryUiState.Empty
                    } else {
                        PaymentHistoryUiState.Content(payments)
                    }
                }
        }
    }
}
