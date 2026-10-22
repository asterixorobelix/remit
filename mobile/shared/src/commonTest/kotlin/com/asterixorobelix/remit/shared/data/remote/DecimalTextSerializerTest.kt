package com.asterixorobelix.remit.shared.data.remote

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DecimalTextSerializerTest {
    private val json = Json

    @Test
    fun writesTheAmountAsABareJsonNumberWithItsExactText() {
        val body = json.encodeToString(CreatePaymentRequestDto("a@example.com", "10.50", "USD"))
        assertEquals("""{"recipientEmail":"a@example.com","amount":10.50,"currency":"USD"}""", body)
    }

    @Test
    fun readsTheNumberLiteralExactlyAsWritten() {
        listOf("10.50", "0.1", "0.30", "12345678901234567.89", "1e3", "-5").forEach { literal ->
            val dto = json.decodeFromString<CreatePaymentRequestDto>(
                """{"recipientEmail":"a@example.com","amount":$literal,"currency":"USD"}""",
            )
            assertEquals(literal, dto.amount)
        }
    }

    @Test
    fun rejectsAQuotedOrNullAmount() {
        listOf("\"10.50\"", "null", "true", "{}").forEach { amount ->
            assertFailsWith<SerializationException>(amount) {
                json.decodeFromString<CreatePaymentRequestDto>(
                    """{"recipientEmail":"a@example.com","amount":$amount,"currency":"USD"}""",
                )
            }
        }
    }

    @Test
    fun refusesToWriteAnythingButAPlainDecimal() {
        listOf("1e3", "10.", "NaN", "1,5", "1}, \"x\": {", "").forEach { text ->
            assertFailsWith<SerializationException>(text) {
                json.encodeToString(CreatePaymentRequestDto("a@example.com", text, "USD"))
            }
        }
    }
}
