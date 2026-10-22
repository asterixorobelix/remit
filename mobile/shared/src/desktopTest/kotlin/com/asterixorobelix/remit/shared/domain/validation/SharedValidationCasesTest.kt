package com.asterixorobelix.remit.shared.domain.validation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Runs testdata/payment-validation-cases.json, the case table the backend tests run too, so the app
 * and the server provably accept and reject the same input.
 */
class SharedValidationCasesTest {
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

    private val cases = Json.decodeFromString<Cases>(
        File(requireNotNull(System.getProperty("validationCases"))).readText(),
    ).cases

    @Test
    fun everySharedCaseMatches() {
        val validator = PaymentValidator()
        val mismatches = cases.mapNotNull { case ->
            val result = validator.validate(PaymentDraft(case.recipientEmail, case.amount, case.currency))
            val actual = when (result) {
                is ValidationResult.Valid -> "valid ${result.request.amount.minorUnits}"
                is ValidationResult.Invalid -> result.errors.map { it.name }.sorted().toString()
            }
            val expected = if (case.expectedErrors.isEmpty()) {
                "valid ${case.expectedMinorUnits}"
            } else {
                case.expectedErrors.sorted().toString()
            }
            "${case.name}: expected $expected, got $actual".takeIf { actual != expected }
        }
        assertTrue(cases.isNotEmpty(), "No cases loaded")
        assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
    }
}
