package com.asterixorobelix.remit.shared.domain.validation

enum class PaymentField { RECIPIENT_EMAIL, AMOUNT, CURRENCY }

/**
 * Why a payment draft is invalid. The names are also the API's error codes (`details` in a 400
 * response), so the app and the server share one vocabulary and the form can highlight the field
 * the server rejected.
 */
enum class ValidationError(val field: PaymentField) {
    EMAIL_BLANK(PaymentField.RECIPIENT_EMAIL),
    EMAIL_TOO_LONG(PaymentField.RECIPIENT_EMAIL),
    EMAIL_INVALID(PaymentField.RECIPIENT_EMAIL),
    AMOUNT_BLANK(PaymentField.AMOUNT),
    AMOUNT_NOT_A_NUMBER(PaymentField.AMOUNT),
    AMOUNT_NOT_POSITIVE(PaymentField.AMOUNT),
    AMOUNT_TOO_MANY_DECIMALS(PaymentField.AMOUNT),
    AMOUNT_ABOVE_LIMIT(PaymentField.AMOUNT),
    CURRENCY_MISSING(PaymentField.CURRENCY),
    CURRENCY_UNSUPPORTED(PaymentField.CURRENCY),
    ;

    companion object {
        fun fromCode(code: String): ValidationError? = entries.firstOrNull { it.name == code }
    }
}
