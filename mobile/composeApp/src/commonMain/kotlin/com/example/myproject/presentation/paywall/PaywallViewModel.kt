package com.example.myproject.presentation.paywall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myproject.domain.purchase.PurchasePremiumUseCase
import com.example.myproject.domain.purchase.PurchaseResult
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.paywall_error_purchase
import com.example.myproject.generated.resources.paywall_error_restore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Purchase state machine for the paywall screen.
 *
 * Handles purchase and restore flows via [PurchasePremiumUseCase].
 * Emits [PaywallEffect.PurchaseComplete] with the original [trigger] on success
 * so the caller can resume the action that was blocked by the paywall.
 *
 * Registered by `purchaseModule` (billing capability) and opened through `AppRoute.Paywall`,
 * which passes the trigger with `parametersOf(route.trigger)`.
 */
class PaywallViewModel(
    private val trigger: String,
    private val purchasePremiumUseCase: PurchasePremiumUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Ready(trigger))
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    private val _effects = Channel<PaywallEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    /** Initiate a purchase of the premium upgrade. */
    fun purchase() {
        viewModelScope.launch {
            _uiState.value = PaywallUiState.Purchasing(trigger)
            handle(
                result = purchasePremiumUseCase(),
                failure = PaywallUiState.Error(trigger, Res.string.paywall_error_purchase),
            )
        }
    }

    /** Restore previous purchases from the store. */
    fun restorePurchases() {
        viewModelScope.launch {
            _uiState.value = PaywallUiState.Restoring(trigger)
            handle(
                result = purchasePremiumUseCase.restore(),
                failure = PaywallUiState.Error(trigger, Res.string.paywall_error_restore),
            )
        }
    }

    private suspend fun handle(result: PurchaseResult, failure: PaywallUiState.Error) {
        when (result) {
            PurchaseResult.Success -> _effects.send(PaywallEffect.PurchaseComplete(trigger))
            PurchaseResult.Cancelled -> _uiState.value = PaywallUiState.Ready(trigger)
            PurchaseResult.Pending -> _uiState.value = PaywallUiState.Pending(trigger)
            is PurchaseResult.Error -> _uiState.value = failure
        }
    }

    /** Reset error state back to Ready so the user can try again. */
    fun retry() {
        _uiState.value = PaywallUiState.Ready(trigger)
    }
}
