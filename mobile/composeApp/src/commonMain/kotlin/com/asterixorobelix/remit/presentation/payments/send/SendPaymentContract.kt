package com.asterixorobelix.remit.presentation.payments.send

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentField
import com.asterixorobelix.remit.shared.domain.validation.ValidationError

/**
 * The Send screen is always a form: there is nothing to load before it can be used, and it is never
 * empty or broken as a whole (failures are field errors or a snackbar). So it has only [Content],
 * kept a sealed interface to match the other screens and leave room for a screen-level state.
 */
sealed interface SendPaymentUiState {
    /**
     * The form. Field errors appear after the first submit and then update as the user types;
     * fields are locked while a payment is being sent.
     */
    data class Content(
        val recipientEmail: String = "",
        val amount: String = "",
        val currency: Currency = Currency.USD,
        val errors: Map<PaymentField, ValidationError> = emptyMap(),
        val isSubmitting: Boolean = false,
        val showErrors: Boolean = false,
        /** The form holds a payment from before a restart that may or may not have gone through. */
        val unfinished: Boolean = false,
    ) : SendPaymentUiState {
        fun toDraft() = PaymentDraft(recipientEmail, amount, currency.code)
    }
}

sealed interface SendPaymentEvent {
    data class RecipientChanged(val value: String) : SendPaymentEvent

    data class AmountChanged(val value: String) : SendPaymentEvent

    data class CurrencySelected(val currency: Currency) : SendPaymentEvent

    /** Send, or retry after a failure: the same details reuse the same idempotency key. */
    data object Submit : SendPaymentEvent

    /** Forget an unfinished payment from before a restart and clear the form. */
    data object DiscardUnfinished : SendPaymentEvent
}

sealed interface SendPaymentEffect {
    /**
     * [alreadySent]: an unfinished payment restored after a restart had gone through before (the
     * server replayed it), so no new payment was made.
     */
    data class PaymentSent(val payment: Payment, val alreadySent: Boolean = false) : SendPaymentEffect

    data class PaymentFailed(val reason: PaymentFailure) : SendPaymentEffect
}
