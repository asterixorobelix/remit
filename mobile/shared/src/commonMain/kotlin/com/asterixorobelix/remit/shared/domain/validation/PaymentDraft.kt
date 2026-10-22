package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.PaymentRequest

/** Form input exactly as typed; [currencyCode] is null until one is picked. */
data class PaymentDraft(val recipientEmail: String, val amount: String, val currencyCode: String?)

sealed interface ValidationResult {
    data class Valid(val request: PaymentRequest) : ValidationResult

    /** At most one error per [PaymentField]. */
    data class Invalid(val errors: Set<ValidationError>) : ValidationResult
}
