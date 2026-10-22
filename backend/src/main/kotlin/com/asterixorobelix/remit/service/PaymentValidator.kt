package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentInstruction
import java.math.BigDecimal

/**
 * Why a payment request is invalid. The names match the mobile app's `ValidationError`, and the
 * tests run testdata/payment-validation-cases.json against both, so client and server agree.
 */
enum class ValidationCode {
    EMAIL_BLANK,
    EMAIL_TOO_LONG,
    EMAIL_INVALID,
    AMOUNT_BLANK,
    AMOUNT_NOT_A_NUMBER,
    AMOUNT_NOT_POSITIVE,
    AMOUNT_TOO_MANY_DECIMALS,
    AMOUNT_ABOVE_LIMIT,
    CURRENCY_MISSING,
    CURRENCY_UNSUPPORTED,
}

sealed interface ValidationOutcome {
    data class Valid(val instruction: PaymentInstruction) : ValidationOutcome

    data class Invalid(val codes: Set<ValidationCode>) : ValidationOutcome
}

/**
 * Server-side payment validation: never trust the client. Amounts arrive as the exact text of the
 * JSON number and are checked with [BigDecimal], so a value with more decimals than the currency
 * allows is rejected instead of rounded.
 */
object PaymentValidator {
    const val MAX_EMAIL_LENGTH = 254
    const val MAX_WHOLE_UNITS = 10_000L
    private const val MAX_LOCAL_PART_LENGTH = 64
    private const val MAX_WHOLE_DIGITS = 13
    private const val MAX_FRACTION_DIGITS = 5
    private const val ATOM = "[A-Za-z0-9!#\$%&'*+/=?^_`{|}~-]+"
    private const val LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
    private val EMAIL = Regex("^$ATOM(?:\\.$ATOM)*@(?:$LABEL\\.)+[A-Za-z]{2,63}$")
    private val DECIMAL = Regex("""^([0-9]*)(?:[.,]([0-9]*))?$""")

    fun validate(recipientEmail: String?, amount: String?, currency: String?): ValidationOutcome {
        val email = email(recipientEmail)
        val code = currency(currency)
        val minor = amountMinor(amount, (code as? Field.Ok)?.value)
        val errors = listOfNotNull(email, minor, code).mapNotNull { (it as? Field.Error)?.code }.toSet()
        return if (email is Field.Ok && minor is Field.Ok && code is Field.Ok) {
            ValidationOutcome.Valid(PaymentInstruction(email.value, minor.value, code.value))
        } else {
            ValidationOutcome.Invalid(errors)
        }
    }

    /** Length is measured before lower-casing, as on the device ("İ" lower-cases to two characters). */
    private fun email(text: String?): Field<String> {
        val trimmed = text.orEmpty().trim()
        val normalized = trimmed.lowercase()
        val plausible = normalized.length <= MAX_EMAIL_LENGTH &&
            normalized.substringBefore('@').length <= MAX_LOCAL_PART_LENGTH &&
            EMAIL.matches(normalized)
        return when {
            trimmed.isEmpty() -> Field.Error(ValidationCode.EMAIL_BLANK)
            trimmed.length > MAX_EMAIL_LENGTH -> Field.Error(ValidationCode.EMAIL_TOO_LONG)
            !plausible -> Field.Error(ValidationCode.EMAIL_INVALID)
            else -> Field.Ok(normalized)
        }
    }

    private fun currency(code: String?): Field<Currency> {
        val currency = code?.let(Currency::fromCode)
        return when {
            code.isNullOrEmpty() -> Field.Error(ValidationCode.CURRENCY_MISSING)
            currency == null -> Field.Error(ValidationCode.CURRENCY_UNSUPPORTED)
            else -> Field.Ok(currency)
        }
    }

    /**
     * Null when the amount is well formed but cannot be priced without a valid currency. Too many
     * whole digits is "above limit"; too many decimals for any currency (a float artefact such as
     * 0.30000000000000004) is "too many decimals".
     */
    private fun amountMinor(text: String?, currency: Currency?): Field<Long>? {
        val trimmed = text.orEmpty().trim()
        val match = DECIMAL.matchEntire(trimmed)
        val whole = match?.groupValues?.get(1).orEmpty()
        val fraction = match?.groupValues?.get(2).orEmpty()
        return when {
            trimmed.isEmpty() -> Field.Error(ValidationCode.AMOUNT_BLANK)

            match == null || (whole.isEmpty() && fraction.isEmpty()) -> Field.Error(ValidationCode.AMOUNT_NOT_A_NUMBER)

            // Digit counts come before BigDecimal: parsing a megabyte of digits takes seconds of CPU.
            whole.trimStart('0').length > MAX_WHOLE_DIGITS -> Field.Error(ValidationCode.AMOUNT_ABOVE_LIMIT)

            fraction.length > MAX_FRACTION_DIGITS -> Field.Error(ValidationCode.AMOUNT_TOO_MANY_DECIMALS)

            else -> exactAmount(whole.trimStart('0'), fraction, currency)
        }
    }

    /** At most [MAX_WHOLE_DIGITS] + [MAX_FRACTION_DIGITS] digits by now, so BigDecimal is cheap. */
    private fun exactAmount(whole: String, fraction: String, currency: Currency?): Field<Long>? {
        val decimal = BigDecimal("${whole.ifEmpty { "0" }}.${fraction}0").setScale(fraction.length)
        return when {
            decimal.signum() == 0 -> Field.Error(ValidationCode.AMOUNT_NOT_POSITIVE)
            currency == null -> null
            else -> priced(decimal, currency)
        }
    }

    private fun priced(decimal: BigDecimal, currency: Currency): Field<Long> {
        val limit = BigDecimal.valueOf(MAX_WHOLE_UNITS)
        return when {
            decimal.scale() > currency.minorDigits -> Field.Error(ValidationCode.AMOUNT_TOO_MANY_DECIMALS)
            decimal > limit -> Field.Error(ValidationCode.AMOUNT_ABOVE_LIMIT)
            else -> Field.Ok(decimal.movePointRight(currency.minorDigits).longValueExact())
        }
    }

    private sealed interface Field<out T> {
        data class Ok<T>(val value: T) : Field<T>

        data class Error(val code: ValidationCode) : Field<Nothing>
    }
}
