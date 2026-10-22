package com.asterixorobelix.remit.shared.data.remote

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.DecimalAmount
import com.asterixorobelix.remit.shared.domain.model.DecimalParseResult
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/** `POST /api/v1/payments` body: `{"recipientEmail": String, "amount": Number, "currency": String}`. */
@Serializable
data class CreatePaymentRequestDto(
    val recipientEmail: String,
    @Serializable(with = DecimalTextSerializer::class) val amount: String,
    val currency: String,
)

@Serializable
data class PaymentResponseDto(
    val id: String,
    val recipientEmail: String,
    @Serializable(with = DecimalTextSerializer::class) val amount: String,
    val currency: String,
    val status: String,
    val createdAt: String,
)

/** Every API error body. Validation failures put [ValidationError] names in [details]. */
@Serializable
data class ErrorResponseDto(
    val error: String,
    val code: String? = null,
    val details: List<String> = emptyList(),
)

internal fun PaymentRequest.toDto() = CreatePaymentRequestDto(
    recipientEmail = recipient.value,
    amount = amount.toDecimalString(),
    currency = amount.currency.code,
)

/** Null when any field is missing or malformed; a payment is never shown with guessed values. */
internal fun PaymentResponseDto.toDomainOrNull(): Payment? {
    val currency = Currency.fromCode(currency)
    val money = (DecimalAmount.parse(amount) as? DecimalParseResult.Parsed)?.amount?.let { decimal ->
        currency?.let(decimal::toMoney)
    }
    val recipient = EmailAddress.parse(recipientEmail)
    val created = parseInstantOrNull(createdAt)
    return Payment.ofOrNull(id, recipient, money, PaymentStatus.fromCode(status), created)
}

private fun parseInstantOrNull(text: String): Instant? = try {
    Instant.parse(text)
} catch (expected: IllegalArgumentException) {
    null
}
