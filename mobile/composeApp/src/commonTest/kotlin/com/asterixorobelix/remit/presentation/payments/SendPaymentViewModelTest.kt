package com.asterixorobelix.remit.presentation.payments

import app.cash.turbine.test
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentEffect
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentEvent
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentUiState
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentViewModel
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.InMemoryPendingPaymentStore
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.usecase.IdempotencyKeys
import com.asterixorobelix.remit.shared.domain.usecase.SendPaymentUseCase
import com.asterixorobelix.remit.shared.domain.usecase.ValidatePaymentUseCase
import com.asterixorobelix.remit.shared.domain.validation.PaymentField
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import com.asterixorobelix.remit.testing.FakePaymentRepository
import com.asterixorobelix.remit.testing.testPayment
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SendPaymentViewModelTest {
    private var keys = 0
    private val store = InMemoryPendingPaymentStore()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: FakePaymentRepository) = SendPaymentViewModel(
        SendPaymentUseCase(PaymentValidator(), repository, IdempotencyKeys(store) { "key-${++keys}" }),
        ValidatePaymentUseCase(PaymentValidator()),
    )

    private val SendPaymentViewModel.form get() = assertIs<SendPaymentUiState.Content>(uiState.value)

    private fun SendPaymentViewModel.fill(email: String = "bob@example.com", amount: String = "10.50") {
        onEvent(SendPaymentEvent.RecipientChanged(email))
        onEvent(SendPaymentEvent.AmountChanged(amount))
    }

    @Test
    fun startsEmptyInDollarsWithoutErrors() {
        val form = viewModel(FakePaymentRepository(SendPaymentResult.Success(testPayment()))).form
        assertEquals(SendPaymentUiState.Content(), form)
        assertEquals(Currency.USD, form.currency)
    }

    @Test
    fun errorsAppearOnlyAfterTheFirstSubmitThenFollowEdits() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)

        vm.fill(email = "bob", amount = "")
        assertTrue(vm.form.errors.isEmpty())

        vm.onEvent(SendPaymentEvent.Submit)
        assertEquals(
            mapOf(
                PaymentField.RECIPIENT_EMAIL to ValidationError.EMAIL_INVALID,
                PaymentField.AMOUNT to ValidationError.AMOUNT_BLANK,
            ),
            vm.form.errors,
        )
        assertTrue(repository.sends.isEmpty())

        vm.onEvent(SendPaymentEvent.RecipientChanged("bob@example.com"))
        assertEquals(mapOf(PaymentField.AMOUNT to ValidationError.AMOUNT_BLANK), vm.form.errors)
    }

    @Test
    fun successResetsTheFormKeepsTheCurrencyAndAnnouncesThePayment() = runTest {
        val sent = testPayment(currency = Currency.EUR)
        val vm = viewModel(FakePaymentRepository(SendPaymentResult.Success(sent)))
        vm.onEvent(SendPaymentEvent.CurrencySelected(Currency.EUR))
        vm.fill()

        vm.effects.test {
            vm.onEvent(SendPaymentEvent.Submit)
            assertEquals(SendPaymentEffect.PaymentSent(sent), awaitItem())
        }
        assertEquals(SendPaymentUiState.Content(currency = Currency.EUR), vm.form)
    }

    @Test
    fun aNewPaymentAfterSuccessGetsANewKeyEvenWithTheSameDetails() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)

        vm.fill()
        vm.onEvent(SendPaymentEvent.Submit)
        vm.fill()
        vm.onEvent(SendPaymentEvent.Submit)

        assertEquals(listOf("key-1", "key-2"), repository.sends.map { it.second })
    }

    @Test
    fun failureKeepsTheFormAndRetryReusesTheKey() = runTest {
        val repository = FakePaymentRepository(
            SendPaymentResult.Failed(PaymentFailure.TIMEOUT),
            SendPaymentResult.Success(testPayment()),
        )
        val vm = viewModel(repository)
        vm.fill()

        vm.effects.test {
            vm.onEvent(SendPaymentEvent.Submit)
            assertEquals(SendPaymentEffect.PaymentFailed(PaymentFailure.TIMEOUT), awaitItem())
            assertEquals("10.50", vm.form.amount)
            assertEquals(false, vm.form.isSubmitting)

            vm.onEvent(SendPaymentEvent.Submit)
            assertIs<SendPaymentEffect.PaymentSent>(awaitItem())
        }
        assertEquals(listOf("key-1", "key-1"), repository.sends.map { it.second })
    }

    @Test
    fun serverRejectionHighlightsTheField() = runTest {
        val vm = viewModel(FakePaymentRepository(SendPaymentResult.Invalid(setOf(ValidationError.EMAIL_INVALID))))
        vm.fill()
        vm.onEvent(SendPaymentEvent.Submit)
        assertEquals(mapOf(PaymentField.RECIPIENT_EMAIL to ValidationError.EMAIL_INVALID), vm.form.errors)
        assertEquals(false, vm.form.isSubmitting)
    }

    @Test
    fun afterARestartAnUnfinishedPaymentIsBackInTheFormWithItsKey() = runTest {
        val repository = FakePaymentRepository(
            SendPaymentResult.Failed(PaymentFailure.TIMEOUT),
            SendPaymentResult.Success(testPayment()),
        )
        val first = viewModel(repository)
        first.onEvent(SendPaymentEvent.CurrencySelected(Currency.EUR))
        first.fill(amount = "7,5")
        first.onEvent(SendPaymentEvent.Submit)

        // The process dies; a new ViewModel over the same store is the reopened app.
        val reopened = viewModel(repository)
        assertEquals(
            SendPaymentUiState.Content(
                recipientEmail = "bob@example.com",
                amount = "7.50",
                currency = Currency.EUR,
                unfinished = true,
            ),
            reopened.form,
        )

        reopened.onEvent(SendPaymentEvent.Submit)
        assertEquals(listOf("key-1", "key-1"), repository.sends.map { it.second })
        assertEquals(false, reopened.form.unfinished)
    }

    @Test
    fun discardingAnUnfinishedPaymentClearsItForGood() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Failed(PaymentFailure.NETWORK))
        viewModel(repository).apply {
            fill()
            onEvent(SendPaymentEvent.Submit)
        }
        val reopened = viewModel(repository)
        assertTrue(reopened.form.unfinished)

        reopened.onEvent(SendPaymentEvent.DiscardUnfinished)

        assertEquals(SendPaymentUiState.Content(), reopened.form)
        assertEquals(SendPaymentUiState.Content(), viewModel(repository).form)
    }

    @Test
    fun editingAnUnfinishedPaymentMakesItANewOne() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Failed(PaymentFailure.NETWORK))
        viewModel(repository).apply {
            fill()
            onEvent(SendPaymentEvent.Submit)
        }
        val reopened = viewModel(repository)
        reopened.onEvent(SendPaymentEvent.AmountChanged("11.00"))
        assertEquals(false, reopened.form.unfinished)

        reopened.onEvent(SendPaymentEvent.Submit)
        assertEquals(listOf("key-1", "key-2"), repository.sends.map { it.second })
    }

    @Test
    fun whileSendingTheFormIsLockedAndASecondSubmitIsIgnored() = runTest {
        val release = CompletableDeferred<Unit>()
        val repository = object : PaymentRepository by FakePaymentRepository(
            SendPaymentResult.Success(testPayment()),
        ) {
            var sends = 0

            override suspend fun send(
                request: PaymentRequest,
                idempotencyKey: String,
            ): SendPaymentResult {
                sends++
                release.await()
                return SendPaymentResult.Success(testPayment())
            }
        }
        val vm = SendPaymentViewModel(
            SendPaymentUseCase(
                PaymentValidator(),
                repository,
                IdempotencyKeys(InMemoryPendingPaymentStore()) {
                    "key"
                },
            ),
            ValidatePaymentUseCase(PaymentValidator()),
        )
        vm.fill()

        vm.onEvent(SendPaymentEvent.Submit)
        assertTrue(vm.form.isSubmitting)
        vm.onEvent(SendPaymentEvent.Submit)
        vm.onEvent(SendPaymentEvent.AmountChanged("99"))
        assertEquals("10.50", vm.form.amount)

        release.complete(Unit)
        assertEquals(1, repository.sends)
        assertEquals(false, vm.form.isSubmitting)
    }
}
