package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Money

object PaymentLimits {
    /** Largest single payment, in the payment's own currency (10,000.00 USD or EUR). */
    const val MAX_WHOLE_UNITS = 10_000L
    private const val DECIMAL_BASE = 10L

    fun maxAmount(currency: Currency): Money =
        Money((1..currency.minorDigits).fold(MAX_WHOLE_UNITS) { value, _ -> value * DECIMAL_BASE }, currency)
}
