package com.asterixorobelix.remit.shared.data.history

import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import kotlin.time.Instant

/**
 * Null for a record with a missing or malformed field: one bad document is skipped instead of
 * breaking the whole history list. Amounts must be positive whole minor units.
 */
internal fun PaymentRecord.toPaymentOrNull(): Payment? {
    val currency = currency?.let(Currency::fromCode)
    val money = amountMinor?.takeIf { it > 0 }?.let { minor -> currency?.let { Money(minor, it) } }
    val recipient = recipientEmail?.let(EmailAddress::parse)
    val createdAt = createdAtEpochMillis?.let(Instant::fromEpochMilliseconds)
    return Payment.ofOrNull(id, recipient, money, PaymentStatus.fromCode(status), createdAt)
}
