package com.asterixorobelix.remit.presentation.payments.send

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.usecase.SendPaymentUseCase
import com.asterixorobelix.remit.shared.domain.usecase.ValidatePaymentUseCase
import com.asterixorobelix.remit.shared.domain.validation.FilterAmountInputUseCase
import com.asterixorobelix.remit.shared.domain.validation.FilterRecipientInputUseCase
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

/**
 * The Send form. What the user typed is kept in [savedState], so it survives the system ending a
 * backgrounded app's process; a payment already sent survives any restart through the pending
 * payment store and comes back as an unfinished payment.
 */
class SendPaymentViewModel(
    private val savedState: SavedStateHandle,
    private val sendPayment: SendPaymentUseCase,
    private val validatePayment: ValidatePaymentUseCase,
    private val filterAmountInput: FilterAmountInputUseCase,
    private val filterRecipientInput: FilterRecipientInputUseCase,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoredForm())
    val uiState: StateFlow<SendPaymentUiState> = mutableState.asStateFlow()

    private val effectChannel = Channel<SendPaymentEffect>(Channel.BUFFERED)
    val effects: Flow<SendPaymentEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch { mutableState.collect(::saveForm) }
        viewModelScope.launch { restoreUnfinishedPayment() }
    }

    fun onEvent(event: SendPaymentEvent) {
        when (event) {
            is SendPaymentEvent.RecipientChanged -> edit {
                val filtered = filterRecipientInput(previous = recipientEmail, proposed = event.value)
                if (filtered == recipientEmail) this else copy(recipientEmail = filtered, unfinished = false)
            }

            is SendPaymentEvent.AmountChanged -> edit {
                val filtered = filterAmountInput(previous = amount, proposed = event.value)
                if (filtered == amount) this else copy(amount = filtered, unfinished = false)
            }

            is SendPaymentEvent.CurrencySelected -> edit { copy(currency = event.currency, unfinished = false) }

            SendPaymentEvent.Submit -> submit()

            SendPaymentEvent.DiscardUnfinished -> discardUnfinished()
        }
    }

    /** After a restart, put the payment that may not have gone through back in the form. */
    private suspend fun restoreUnfinishedPayment() {
        val request = sendPayment.unfinishedPayment()?.request ?: return
        mutableState.update { current ->
            when {
                current == SendPaymentUiState.Content() -> SendPaymentUiState.Content(
                    recipientEmail = request.recipient.value,
                    amount = request.amount.toDecimalString(),
                    currency = request.amount.currency,
                    unfinished = true,
                )

                // The process ended mid-send and the restored form still holds that payment.
                !current.isSubmitting && current.request() == request -> current.copy(unfinished = true)

                else -> current
            }
        }
    }

    private fun restoredForm(): SendPaymentUiState.Content {
        val form = SendPaymentUiState.Content(
            recipientEmail = savedState[KEY_RECIPIENT] ?: "",
            amount = savedState[KEY_AMOUNT] ?: "",
            currency = savedState.get<String>(KEY_CURRENCY)?.let(Currency::fromCode) ?: Currency.USD,
            showErrors = savedState[KEY_SHOW_ERRORS] ?: false,
            unfinished = savedState[KEY_UNFINISHED] ?: false,
        )
        return if (form.showErrors) form.copy(errors = errorsFor(form)) else form
    }

    /** Errors are derived and a send in flight ends with the process, so neither is saved. */
    private fun saveForm(form: SendPaymentUiState.Content) {
        savedState[KEY_RECIPIENT] = form.recipientEmail
        savedState[KEY_AMOUNT] = form.amount
        savedState[KEY_CURRENCY] = form.currency.code
        savedState[KEY_SHOW_ERRORS] = form.showErrors
        savedState[KEY_UNFINISHED] = form.unfinished
    }

    /** Clears the form at once, so a Send tapped while the key is being forgotten sends nothing. */
    private fun discardUnfinished() {
        if (mutableState.value.isSubmitting) return
        mutableState.update { SendPaymentUiState.Content(currency = it.currency) }
        viewModelScope.launch { sendPayment.startNewPayment() }
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
        mutableState.value = current.copy(
            isSubmitting = true,
            showErrors = true,
            unfinished = false,
            errors = errorsFor(current),
        )
        viewModelScope.launch {
            when (val result = sendPayment(current.toDraft())) {
                is SendPaymentResult.Success -> {
                    sendPayment.startNewPayment()
                    mutableState.value = SendPaymentUiState.Content(currency = current.currency)
                    // A replay also answers an automatic retry of this very tap (the first attempt
                    // went through but its response was lost); only a payment restored after a
                    // restart was sent "earlier" as far as the user knows.
                    val alreadySent = result.replayed && current.unfinished
                    effectChannel.send(SendPaymentEffect.PaymentSent(result.payment, alreadySent))
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

    private fun SendPaymentUiState.Content.request(): PaymentRequest? =
        (validatePayment(toDraft()) as? ValidationResult.Valid)?.request

    private fun errorsFor(state: SendPaymentUiState.Content): Map<PaymentField, ValidationError> =
        (validatePayment(state.toDraft()) as? ValidationResult.Invalid)?.errors?.byField().orEmpty()

    private fun Set<ValidationError>.byField(): Map<PaymentField, ValidationError> = associateBy { it.field }

    private companion object {
        const val KEY_RECIPIENT = "recipientEmail"
        const val KEY_AMOUNT = "amount"
        const val KEY_CURRENCY = "currency"
        const val KEY_SHOW_ERRORS = "showErrors"
        const val KEY_UNFINISHED = "unfinished"
    }
}
