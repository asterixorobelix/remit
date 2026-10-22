package com.asterixorobelix.remit.shared.data.remote

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
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

/**
 * Carries decimal text as a bare JSON number, the API's wire format (`"amount": 10.50`), without
 * ever converting it to `Double`: writing emits the text verbatim, and reading returns the number
 * exactly as it appeared in the JSON. Only plain decimals are written, so the unquoted literal can
 * never inject other JSON.
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
            ?: throw SerializationException("Expected a JSON number, got $element")
        return number.content
    }
}
