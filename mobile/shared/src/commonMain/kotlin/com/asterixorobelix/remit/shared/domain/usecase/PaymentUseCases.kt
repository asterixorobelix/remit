package com.asterixorobelix.remit.shared.domain.usecase

import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.repository.PendingPayment
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import com.asterixorobelix.remit.shared.domain.validation.ValidationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Validates a draft and sends it. Calls run one at a time, so a second tap waits for the first and
 * then reuses its idempotency key: the server returns the same payment rather than a second one.
 */
class SendPaymentUseCase(
    private val validator: PaymentValidator,
    private val repository: PaymentRepository,
    private val keys: IdempotencyKeys,
) {
    private val mutex = Mutex()

    suspend operator fun invoke(draft: PaymentDraft): SendPaymentResult = mutex.withLock {
        when (val validation = validator.validate(draft)) {
            is ValidationResult.Invalid -> SendPaymentResult.Invalid(validation.errors)

            is ValidationResult.Valid -> {
                val result = repository.send(validation.request, keys.keyFor(validation.request))
                // Never processed (server rejected it) or the key is unusable: nothing to finish later.
                if (result is SendPaymentResult.Invalid || result == CONFLICT) keys.reset()
                result
            }
        }
    }

    private companion object {
        val CONFLICT = SendPaymentResult.Failed(PaymentFailure.IDEMPOTENCY_CONFLICT)
    }

    /** The next submit is a new payment, even with identical details. */
    suspend fun startNewPayment() = mutex.withLock { keys.reset() }

    /**
     * The payment last sent that the user has not moved on from. After a restart it may have gone
     * through or not; sending it again is safe because it reuses its idempotency key.
     */
    suspend fun unfinishedPayment(): PendingPayment? = mutex.withLock { keys.pending() }
}

/** Validates without sending, for showing field errors as the user edits. */
class ValidatePaymentUseCase(private val validator: PaymentValidator) {
    operator fun invoke(draft: PaymentDraft): ValidationResult = validator.validate(draft)
}

class ObservePaymentHistoryUseCase(private val repository: PaymentRepository) {
    operator fun invoke(): Flow<List<Payment>> = repository.observeHistory()
}
