package com.asterixorobelix.remit.domain

import java.time.Instant

enum class Currency(val code: String, val minorDigits: Int) {
    USD(code = "USD", minorDigits = 2),
    EUR(code = "EUR", minorDigits = 2),
    ;

    companion object {
        /** Exact, case-sensitive ISO 4217 lookup, as on the device. */
        fun fromCode(code: String): Currency? = entries.firstOrNull { it.code == code }
    }
}

/** The mock processor approves every valid payment; the field exists for real outcomes. */
enum class PaymentStatus { COMPLETED, }

/** A validated payment instruction. [amountMinor] is whole cents; money never touches `Double`. */
data class PaymentInstruction(
    val recipientEmail: String,
    val amountMinor: Long,
    val currency: Currency,
)

/** A processed payment as stored in Firestore (`payments/{id}`). */
data class StoredPayment(
    val id: String,
    val senderUid: String,
    val recipientEmail: String,
    val amountMinor: Long,
    val currency: Currency,
    val status: PaymentStatus,
    val createdAt: Instant,
)
