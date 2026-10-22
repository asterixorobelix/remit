package com.asterixorobelix.remit.presentation.payments.send

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentField
import com.asterixorobelix.remit.shared.domain.validation.ValidationError

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
}

sealed interface SendPaymentEffect {
    data class PaymentSent(val payment: Payment) : SendPaymentEffect

    data class PaymentFailed(val reason: PaymentFailure) : SendPaymentEffect
}
