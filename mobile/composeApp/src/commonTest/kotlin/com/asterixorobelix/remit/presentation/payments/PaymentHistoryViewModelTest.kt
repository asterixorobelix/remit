package com.asterixorobelix.remit.presentation.payments

import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryEvent
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryUiState
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryViewModel
import com.asterixorobelix.remit.shared.data.history.PaymentHistoryUnavailableException
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.usecase.ObservePaymentHistoryUseCase
import com.asterixorobelix.remit.testing.FakePaymentRepository
import com.asterixorobelix.remit.testing.RecordingCrashLogger
import com.asterixorobelix.remit.testing.testPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentHistoryViewModelTest {
    private val crashLogger = RecordingCrashLogger()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(repository: PaymentRepository) =
        PaymentHistoryViewModel(ObservePaymentHistoryUseCase(repository), crashLogger)

    @Test
    fun loadsThenShowsLiveUpdates() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)
        assertEquals(PaymentHistoryUiState.Loading, vm.uiState.value)

        repository.history.emit(listOf(testPayment("pay_1")))
        assertEquals(listOf("pay_1"), assertIs<PaymentHistoryUiState.Content>(vm.uiState.value).payments.map { it.id })

        // A payment recorded elsewhere arrives through the listener without any action.
        repository.history.emit(listOf(testPayment("pay_2"), testPayment("pay_1")))
        assertEquals(
            listOf("pay_2", "pay_1"),
            assertIs<PaymentHistoryUiState.Content>(vm.uiState.value).payments.map { it.id },
        )
    }

    @Test
    fun noPaymentsIsEmptyNotAnError() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)
        repository.history.emit(emptyList())
        assertEquals(PaymentHistoryUiState.Empty, vm.uiState.value)
    }

    @Test
    fun listenerFailureIsAnErrorAndIsReported() = runTest {
        val vm = viewModel(FailingHistory())
        assertEquals(PaymentHistoryUiState.Error, vm.uiState.value)
        assertIs<PaymentHistoryUnavailableException>(crashLogger.exceptions.single())
    }

    @Test
    fun refreshReconnectsTheListener() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)
        repository.history.emit(listOf(testPayment()))

        vm.onEvent(PaymentHistoryEvent.Refresh)

        assertEquals(2, repository.historyCollections)
        assertEquals(listOf(testPayment()), assertIs<PaymentHistoryUiState.Content>(vm.uiState.value).payments)
    }

    @Test
    fun refreshAfterAnErrorShowsLoadingThenRecovers() = runTest {
        val repository = FailingHistory(failures = 1)
        val vm = viewModel(repository)
        assertEquals(PaymentHistoryUiState.Error, vm.uiState.value)

        vm.onEvent(PaymentHistoryEvent.Refresh)

        assertEquals(PaymentHistoryUiState.Empty, vm.uiState.value)
    }

    private class FailingHistory(private var failures: Int = Int.MAX_VALUE) : PaymentRepository {
        override suspend fun send(
            request: PaymentRequest,
            idempotencyKey: String,
        ): SendPaymentResult = error("Not used")

        override fun observeHistory(): Flow<List<Payment>> = flow {
            if (failures-- > 0) throw PaymentHistoryUnavailableException()
            emit(emptyList())
        }
    }
}
