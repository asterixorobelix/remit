package com.asterixorobelix.remit.shared.domain.usecase

import app.cash.turbine.test
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import com.asterixorobelix.remit.shared.domain.validation.ValidationResult
import com.asterixorobelix.remit.shared.testing.FakePaymentRepository
import com.asterixorobelix.remit.shared.testing.payment
import com.asterixorobelix.remit.shared.testing.paymentRequest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class OtherUseCasesTest {
    @Test
    fun validateDelegatesToTheValidator() {
        val validate = ValidatePaymentUseCase(PaymentValidator())
        assertIs<ValidationResult.Valid>(validate(PaymentDraft("a@example.com", "1", "EUR")))
        assertIs<ValidationResult.Invalid>(validate(PaymentDraft("a@example.com", "", "EUR")))
    }

    @Test
    fun observeHistoryStreamsTheRepository() = runTest {
        val repository = FakePaymentRepository()
        ObservePaymentHistoryUseCase(repository)().test {
            repository.history.emit(listOf(payment()))
            assertEquals(listOf(payment()), awaitItem())
        }
    }

    @Test
    fun keysAreStablePerRequestAndResettable() {
        var counter = 0
        val keys = IdempotencyKeys { "k${counter++}" }
        val request = paymentRequest()

        val first = keys.keyFor(request)
        assertEquals(first, keys.keyFor(request))
        assertNotEquals(first, keys.keyFor(paymentRequest(recipient = "bob@example.com")))
        keys.reset()
        assertNotEquals(first, keys.keyFor(request))
    }
}
