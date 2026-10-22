package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentInstruction
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

class PaymentValidatorTest {
    @Serializable
    private data class Case(
        val name: String,
        val recipientEmail: String,
        val amount: String,
        val currency: String?,
        val expectedErrors: List<String>,
        val expectedMinorUnits: Long? = null,
    )

    @Serializable
    private data class Cases(val description: String, val cases: List<Case>)

    /** testdata/payment-validation-cases.json: the same table the mobile shared module runs. */
    @Test
    fun agreesWithTheAppOnEverySharedCase() {
        val cases = Json.decodeFromString<Cases>(File(System.getProperty("validationCases")).readText()).cases
        val mismatches = cases.mapNotNull { case ->
            val actual = when (
                val outcome = PaymentValidator.validate(
                    case.recipientEmail,
                    case.amount,
                    case.currency,
                )
            ) {
                is ValidationOutcome.Valid -> "valid ${outcome.instruction.amountMinor}"
                is ValidationOutcome.Invalid -> outcome.codes.map { it.name }.sorted().toString()
            }
            val expected = if (case.expectedErrors.isEmpty()) {
                "valid ${case.expectedMinorUnits}"
            } else {
                case.expectedErrors.sorted().toString()
            }
            "${case.name}: expected $expected, got $actual".takeIf { actual != expected }
        }
        assertTrue(cases.size > 50, "Expected the full case table, got ${cases.size}")
        assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
    }

    @Test
    fun normalizesTheRecipientAndConvertsToCents() {
        val outcome = PaymentValidator.validate("  Bob@Example.COM ", "10.5", "EUR")
        assertEquals(ValidationOutcome.Valid(PaymentInstruction("bob@example.com", 1050, Currency.EUR)), outcome)
    }

    @Test
    fun missingFieldsAreValidationCodesNotCrashes() {
        assertEquals(
            ValidationOutcome.Invalid(
                setOf(ValidationCode.EMAIL_BLANK, ValidationCode.AMOUNT_BLANK, ValidationCode.CURRENCY_MISSING),
            ),
            PaymentValidator.validate(null, null, null),
        )
    }

    @Test
    fun hugeAmountsAreRejectedWithoutParsingThem() {
        val digits = "9".repeat(1_000_000)
        val elapsed = measureTime {
            assertEquals(
                ValidationOutcome.Invalid(setOf(ValidationCode.AMOUNT_ABOVE_LIMIT)),
                PaymentValidator.validate("a@example.com", digits, "USD"),
            )
            assertEquals(
                ValidationOutcome.Invalid(setOf(ValidationCode.AMOUNT_TOO_MANY_DECIMALS)),
                PaymentValidator.validate("a@example.com", "1.$digits", "USD"),
            )
        }
        // The digit checks take milliseconds; parsing one of these with BigDecimal takes over ten
        // seconds. The wide bound keeps a slow or busy CI runner from failing the test.
        assertTrue(elapsed < 5.seconds, "Took $elapsed")
    }

    @Test
    fun jsonNumberFormsOutsidePlainDecimalsAreRejected() {
        listOf("1e3", "1E-2", "-1e3", "1.5e1").forEach { literal ->
            assertEquals(
                ValidationOutcome.Invalid(setOf(ValidationCode.AMOUNT_NOT_A_NUMBER)),
                PaymentValidator.validate("a@example.com", literal, "USD"),
                literal,
            )
        }
    }
}
