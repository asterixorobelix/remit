package com.asterixorobelix.remit.presentation.payments.send

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.usecase.SendPaymentUseCase
import com.asterixorobelix.remit.shared.domain.usecase.ValidatePaymentUseCase
import com.asterixorobelix.remit.shared.domain.validation.PaymentField
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import com.asterixorobelix.remit.shared.domain.validation.ValidationResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SendPaymentViewModel(
    private val sendPayment: SendPaymentUseCase,
    private val validatePayment: ValidatePaymentUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SendPaymentUiState.Content())
    val uiState: StateFlow<SendPaymentUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<SendPaymentEffect>(Channel.BUFFERED)
    val effects: Flow<SendPaymentEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: SendPaymentEvent) {
        when (event) {
            is SendPaymentEvent.RecipientChanged -> edit { copy(recipientEmail = event.value) }
            is SendPaymentEvent.AmountChanged -> edit { copy(amount = event.value) }
            is SendPaymentEvent.CurrencySelected -> edit { copy(currency = event.currency) }
            SendPaymentEvent.Submit -> submit()
        }
    }

    private fun edit(change: SendPaymentUiState.Content.() -> SendPaymentUiState.Content) {
        mutableState.update { current ->
            if (current.isSubmitting) {
                current
            } else {
                current.change().let { next -> if (next.showErrors) next.copy(errors = errorsFor(next)) else next }
            }
        }
    }

    private fun submit() {
        val current = mutableState.value
        if (current.isSubmitting) return
        mutableState.value = current.copy(isSubmitting = true, showErrors = true, errors = errorsFor(current))
        viewModelScope.launch {
            when (val result = sendPayment(current.toDraft())) {
                is SendPaymentResult.Success -> {
                    sendPayment.startNewPayment()
                    mutableState.value = SendPaymentUiState.Content(currency = current.currency)
                    effectChannel.send(SendPaymentEffect.PaymentSent(result.payment))
                }

                is SendPaymentResult.Invalid -> mutableState.update {
                    it.copy(isSubmitting = false, errors = result.errors.byField())
                }

                is SendPaymentResult.Failed -> {
                    mutableState.update { it.copy(isSubmitting = false) }
                    effectChannel.send(SendPaymentEffect.PaymentFailed(result.reason))
                }
            }
        }
    }

    private fun errorsFor(state: SendPaymentUiState.Content): Map<PaymentField, ValidationError> =
        (validatePayment(state.toDraft()) as? ValidationResult.Invalid)?.errors?.byField().orEmpty()

    private fun Set<ValidationError>.byField(): Map<PaymentField, ValidationError> = associateBy { it.field }
}
