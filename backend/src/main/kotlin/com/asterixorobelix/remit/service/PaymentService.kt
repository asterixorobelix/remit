package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.domain.PaymentInstruction
import com.asterixorobelix.remit.domain.PaymentStatus
import com.asterixorobelix.remit.domain.StoredPayment
import com.asterixorobelix.remit.dto.CreatePaymentRequest
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.HexFormat
import java.util.UUID

sealed interface PaymentResult {
    data class Created(val payment: StoredPayment) : PaymentResult

    data class Replayed(val payment: StoredPayment) : PaymentResult

    data class Invalid(val codes: Set<ValidationCode>) : PaymentResult

    data object KeyReused : PaymentResult
}

/**
 * Processes a payment: validate, then record it exactly once per idempotency key. Processing is
 * mocked (every valid payment completes); a real processor call would go between validation and
 * the store, keyed by the same idempotency key so the processor deduplicates too.
 */
class PaymentService(
    private val store: PaymentStore,
    private val clock: Clock = Clock.systemUTC(),
    private val newId: () -> String = { "pay_" + UUID.randomUUID().toString().replace("-", "") },
) {
    suspend fun create(senderUid: String, idempotencyKey: String, request: CreatePaymentRequest): PaymentResult =
        when (val validation = PaymentValidator.validate(request.recipientEmail, request.amount, request.currency)) {
            is ValidationOutcome.Invalid -> PaymentResult.Invalid(validation.codes)
            is ValidationOutcome.Valid -> record(senderUid, idempotencyKey, validation.instruction)
        }

    private suspend fun record(
        senderUid: String,
        idempotencyKey: String,
        instruction: PaymentInstruction,
    ): PaymentResult {
        val payment = StoredPayment(
            id = newId(),
            senderUid = senderUid,
            recipientEmail = instruction.recipientEmail,
            amountMinor = instruction.amountMinor,
            currency = instruction.currency,
            status = PaymentStatus.COMPLETED,
            // Millisecond precision, so a replay read back from Firestore matches the original response.
            createdAt = Instant.now(clock).truncatedTo(ChronoUnit.MILLIS),
        )
        return when (val outcome = store.createOnce(idempotencyKey, fingerprint(instruction), payment)) {
            is CreateOutcome.Created -> PaymentResult.Created(outcome.payment)
            is CreateOutcome.Replayed -> PaymentResult.Replayed(outcome.payment)
            CreateOutcome.KeyReused -> PaymentResult.KeyReused
        }
    }

    /** What makes two requests "the same payment": recipient, exact amount, and currency. */
    private fun fingerprint(instruction: PaymentInstruction): String {
        val canonical = "${instruction.recipientEmail}|${instruction.amountMinor}|${instruction.currency.code}"
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()))
    }
}
