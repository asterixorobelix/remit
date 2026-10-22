package com.asterixorobelix.remit.shared.domain.validation

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.AMOUNT_ABOVE_LIMIT
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.AMOUNT_BLANK
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.AMOUNT_NOT_A_NUMBER
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.AMOUNT_NOT_POSITIVE
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.AMOUNT_TOO_MANY_DECIMALS
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.CURRENCY_MISSING
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.CURRENCY_UNSUPPORTED
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.EMAIL_BLANK
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.EMAIL_INVALID
import com.asterixorobelix.remit.shared.domain.validation.ValidationError.EMAIL_TOO_LONG
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The full shared case table (testdata/payment-validation-cases.json) runs in desktopTest, where
 * files can be read; these cover each rule directly on every platform, iOS included.
 */
class PaymentValidatorTest {
    private val validator = PaymentValidator()

    private fun errors(email: String = "alice@example.com", amount: String = "10", currency: String? = "USD") =
        (validator.validate(PaymentDraft(email, amount, currency)) as? ValidationResult.Invalid)?.errors.orEmpty()

    @Test
    fun validDraftBecomesARequestWithExactMinorUnits() {
        val result = validator.validate(PaymentDraft(" Bob@Example.com ", "10,05", "EUR"))
        val request = (result as ValidationResult.Valid).request
        assertEquals("bob@example.com", request.recipient.value)
        assertEquals(Money(1005, Currency.EUR), request.amount)
    }

    @Test
    fun emailRules() {
        assertEquals(setOf(EMAIL_BLANK), errors(email = " "))
        assertEquals(setOf(EMAIL_INVALID), errors(email = "alice"))
        assertEquals(setOf(EMAIL_TOO_LONG), errors(email = "a".repeat(250) + "@x.com"))
    }

    @Test
    fun amountRules() {
        assertEquals(setOf(AMOUNT_BLANK), errors(amount = ""))
        assertEquals(setOf(AMOUNT_NOT_A_NUMBER), errors(amount = "-1"))
        assertEquals(setOf(AMOUNT_NOT_POSITIVE), errors(amount = "0.00"))
        assertEquals(setOf(AMOUNT_TOO_MANY_DECIMALS), errors(amount = "1.234"))
        assertEquals(setOf(AMOUNT_ABOVE_LIMIT), errors(amount = "10000.01"))
        assertEquals(setOf(AMOUNT_ABOVE_LIMIT), errors(amount = "9".repeat(30)))
        assertEquals(emptySet(), errors(amount = "10000.00"))
    }

    @Test
    fun currencyRules() {
        assertEquals(setOf(CURRENCY_MISSING), errors(currency = null))
        assertEquals(setOf(CURRENCY_MISSING), errors(currency = ""))
        assertEquals(setOf(CURRENCY_UNSUPPORTED), errors(currency = "usd"))
        assertEquals(setOf(CURRENCY_UNSUPPORTED), errors(currency = "JPY"))
    }

    @Test
    fun reportsEveryFieldAtOnce() {
        assertEquals(
            setOf(EMAIL_BLANK, AMOUNT_BLANK, CURRENCY_MISSING),
            errors(email = "", amount = "", currency = null),
        )
        assertEquals(setOf(EMAIL_INVALID, AMOUNT_NOT_POSITIVE), errors(email = "x", amount = "0"))
    }

    @Test
    fun amountChecksThatNeedACurrencyWaitForOne() {
        assertEquals(setOf(CURRENCY_MISSING), errors(amount = "0.001", currency = null))
        assertEquals(setOf(CURRENCY_UNSUPPORTED), errors(amount = "99999", currency = "GBP"))
        assertEquals(setOf(AMOUNT_NOT_POSITIVE, CURRENCY_MISSING), errors(amount = "0", currency = null))
    }

    @Test
    fun limitIsTenThousandInEachCurrency() {
        assertEquals(Money(1_000_000, Currency.USD), PaymentLimits.maxAmount(Currency.USD))
        assertEquals(Money(1_000_000, Currency.EUR), PaymentLimits.maxAmount(Currency.EUR))
    }

    @Test
    fun errorCodesRoundTrip() {
        ValidationError.entries.forEach { assertEquals(it, ValidationError.fromCode(it.name)) }
        assertEquals(null, ValidationError.fromCode("SOMETHING_NEW"))
    }
}
