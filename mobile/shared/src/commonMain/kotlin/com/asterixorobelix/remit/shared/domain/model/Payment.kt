package com.asterixorobelix.remit.shared.domain.model

import kotlin.time.Instant

/** A validated payment, ready to send. */
data class PaymentRequest(val recipient: EmailAddress, val amount: Money)

/** A payment the server has processed, as shown in history. */
data class Payment(
    val id: String,
    val recipient: EmailAddress,
    val amount: Money,
    val status: PaymentStatus,
    val createdAt: Instant,
) {
    companion object {
        /** Null unless every part is present, so mappers drop a record with any bad field. */
        fun ofOrNull(
            id: String,
            recipient: EmailAddress?,
            amount: Money?,
            status: PaymentStatus,
            createdAt: Instant?,
        ): Payment? = if (recipient == null || amount == null || createdAt == null) {
            null
        } else {
            Payment(id, recipient, amount, status, createdAt).takeIf { id.isNotBlank() }
        }
    }
}

enum class PaymentStatus {
    COMPLETED,
    PENDING,
    FAILED,

    /** A status this app version does not know yet; shown as-is rather than failing the list. */
    UNKNOWN,
    ;

    companion object {
        fun fromCode(code: String?): PaymentStatus = entries.firstOrNull { it != UNKNOWN && it.name == code } ?: UNKNOWN
    }
}
