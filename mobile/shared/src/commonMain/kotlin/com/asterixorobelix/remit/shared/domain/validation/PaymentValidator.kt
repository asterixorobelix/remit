package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.DecimalAmount
import com.asterixorobelix.remit.shared.domain.model.DecimalParseResult
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest

/**
 * Turns form input into a [PaymentRequest] or the set of problems with it, one per field, so the
 * form can show every error at once. The backend applies the same rules (see
 * `testdata/payment-validation-cases.json`, which both test suites run).
 */
class PaymentValidator {
    fun validate(draft: PaymentDraft): ValidationResult {
        val email = validateEmail(draft.recipientEmail)
        val currency = validateCurrency(draft.currencyCode)
        val amount = validateAmount(draft.amount, (currency as? FieldResult.Ok)?.value)
        val request = if (email is FieldResult.Ok && amount is FieldResult.Ok) {
            PaymentRequest(email.value, amount.value)
        } else {
            null
        }
        val errors = listOfNotNull(email, amount, currency)
            .mapNotNull { (it as? FieldResult.Error)?.error }
            .toSet()
        return request?.let(ValidationResult::Valid) ?: ValidationResult.Invalid(errors)
    }

    private fun validateEmail(text: String): FieldResult<EmailAddress> {
        val trimmed = text.trim()
        val address = EmailAddress.parse(trimmed)
        return when {
            trimmed.isEmpty() -> FieldResult.Error(ValidationError.EMAIL_BLANK)
            trimmed.length > EmailAddress.MAX_LENGTH -> FieldResult.Error(ValidationError.EMAIL_TOO_LONG)
            address == null -> FieldResult.Error(ValidationError.EMAIL_INVALID)
            else -> FieldResult.Ok(address)
        }
    }

    private fun validateCurrency(code: String?): FieldResult<Currency> {
        val currency = code?.let(Currency::fromCode)
        return when {
            code.isNullOrEmpty() -> FieldResult.Error(ValidationError.CURRENCY_MISSING)
            currency == null -> FieldResult.Error(ValidationError.CURRENCY_UNSUPPORTED)
            else -> FieldResult.Ok(currency)
        }
    }

    /** Null when the amount is well formed but cannot be priced because the currency is invalid. */
    private fun validateAmount(text: String, currency: Currency?): FieldResult<Money>? =
        when (val parsed = DecimalAmount.parse(text)) {
            DecimalParseResult.Failure.BLANK -> FieldResult.Error(ValidationError.AMOUNT_BLANK)
            DecimalParseResult.Failure.NOT_A_NUMBER -> FieldResult.Error(ValidationError.AMOUNT_NOT_A_NUMBER)
            DecimalParseResult.Failure.NEGATIVE -> FieldResult.Error(ValidationError.AMOUNT_NOT_POSITIVE)
            DecimalParseResult.Failure.OUT_OF_RANGE -> FieldResult.Error(ValidationError.AMOUNT_ABOVE_LIMIT)
            DecimalParseResult.Failure.TOO_PRECISE -> FieldResult.Error(ValidationError.AMOUNT_TOO_MANY_DECIMALS)
            is DecimalParseResult.Parsed -> priceAmount(parsed.amount, currency)
        }

    private fun priceAmount(amount: DecimalAmount, currency: Currency?): FieldResult<Money>? {
        val money = currency?.let(amount::toMoney)
        return when {
            amount.isZero -> FieldResult.Error(ValidationError.AMOUNT_NOT_POSITIVE)
            currency == null -> null
            money == null -> FieldResult.Error(ValidationError.AMOUNT_TOO_MANY_DECIMALS)
            money > PaymentLimits.maxAmount(currency) -> FieldResult.Error(ValidationError.AMOUNT_ABOVE_LIMIT)
            else -> FieldResult.Ok(money)
        }
    }
}

private sealed interface FieldResult<out T> {
    data class Ok<T>(val value: T) : FieldResult<T>

    data class Error(val error: ValidationError) : FieldResult<Nothing>
}
