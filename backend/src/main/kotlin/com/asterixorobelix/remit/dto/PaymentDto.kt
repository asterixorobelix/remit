package com.asterixorobelix.remit.dto

import com.asterixorobelix.remit.domain.StoredPayment
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import java.math.BigDecimal

/**
 * `POST /api/v1/payments` body: `{"recipientEmail": String, "amount": Number, "currency": String}`.
 * Fields are optional here so a missing one is reported as a validation code, not a parse error.
 */
@Serializable
data class CreatePaymentRequest(
    val recipientEmail: String? = null,
    @Serializable(with = DecimalTextSerializer::class) val amount: String? = null,
    val currency: String? = null,
)

@Serializable
data class PaymentResponse(
    val id: String,
    val recipientEmail: String,
    @Serializable(with = DecimalTextSerializer::class) val amount: String,
    val currency: String,
    val status: String,
    val createdAt: String,
)

fun StoredPayment.toResponse() = PaymentResponse(
    id = id,
    recipientEmail = recipientEmail,
    amount = BigDecimal.valueOf(amountMinor, currency.minorDigits).toPlainString(),
    currency = currency.code,
    status = status.name,
    createdAt = createdAt.toString(),
)

/**
 * The amount as the exact text of a JSON number, in both directions, so it never becomes a
 * `Double`: `10.50` is read as "10.50" and written back as `10.50`. A quoted amount is rejected.
 */
object DecimalTextSerializer : KSerializer<String> {
    private val PLAIN_DECIMAL = Regex("""^-?[0-9]+(\.[0-9]+)?$""")

    /** RFC 8259 number grammar: an unquoted `true` or `null` is a literal too, but not a number. */
    private val JSON_NUMBER = Regex("""^-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?$""")

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.asterixorobelix.remit.DecimalText", PrimitiveKind.DOUBLE)

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: String) {
        val json = encoder as? JsonEncoder ?: throw SerializationException("Decimal text supports JSON only")
        if (!PLAIN_DECIMAL.matches(value)) throw SerializationException("Not a plain decimal: $value")
        json.encodeJsonElement(JsonUnquotedLiteral(value))
    }

    override fun deserialize(decoder: Decoder): String {
        val json = decoder as? JsonDecoder ?: throw SerializationException("Decimal text supports JSON only")
        val element = json.decodeJsonElement()
        val number = (element as? JsonPrimitive)?.takeIf { !it.isString && JSON_NUMBER.matches(it.content) }
            ?: throw SerializationException("amount must be a JSON number")
        return number.content
    }
}
