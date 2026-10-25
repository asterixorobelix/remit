package com.asterixorobelix.remit.presentation.payments

import com.asterixorobelix.remit.presentation.payments.history.HISTORY_STOP_TIMEOUT_MS
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryEvent
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryUiState
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryViewModel
import com.asterixorobelix.remit.presentation.payments.history.SetupHelp
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.HistoryFailure
import com.asterixorobelix.remit.shared.domain.repository.PaymentHistoryException
import com.asterixorobelix.remit.shared.domain.repository.PaymentRepository
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.usecase.ObservePaymentHistoryUseCase
import com.asterixorobelix.remit.testing.FakeAppConfig
import com.asterixorobelix.remit.testing.FakePaymentRepository
import com.asterixorobelix.remit.testing.RecordingCrashLogger
import com.asterixorobelix.remit.testing.testPayment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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

    private fun viewModel(repository: PaymentRepository, config: FakeAppConfig = FakeAppConfig(isDebug = false)) =
        PaymentHistoryViewModel(ObservePaymentHistoryUseCase(repository), crashLogger, config)

    /** The screen on display: collects the state until [cancel][Job.cancel] or the end of the test. */
    private fun TestScope.show(vm: PaymentHistoryViewModel): Job =
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

    private fun TestScope.shown(repository: PaymentRepository, config: FakeAppConfig = FakeAppConfig(isDebug = false)) =
        viewModel(repository, config).also { show(it) }

    @Test
    fun loadsThenShowsLiveUpdates() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = shown(repository)
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
        val vm = shown(repository)
        repository.history.emit(emptyList())
        assertEquals(PaymentHistoryUiState.Empty, vm.uiState.value)
    }

    @Test
    fun listenerFailureIsAnErrorWithItsReasonAndIsReported() = runTest {
        val vm = shown(FailingHistory(reason = HistoryFailure.NETWORK))
        assertEquals(PaymentHistoryUiState.Error(HistoryFailure.NETWORK), vm.uiState.value)
        assertIs<PaymentHistoryException>(crashLogger.exceptions.single())
    }

    @Test
    fun anUnexpectedFailureIsNotBlamedOnTheConnection() = runTest {
        val vm = shown(FailingHistory(failure = IllegalStateException("boom")))
        assertEquals(PaymentHistoryUiState.Error(HistoryFailure.UNAVAILABLE), vm.uiState.value)
    }

    @Test
    fun aBuildWithoutFirebaseShowsSetupHelpOnlyInDebugBuilds() = runTest {
        val notSetUp = FailingHistory(reason = HistoryFailure.NOT_SET_UP)
        assertEquals(
            PaymentHistoryUiState.Error(HistoryFailure.NOT_SET_UP, SetupHelp.ConnectFirebase),
            shown(notSetUp, FakeAppConfig(isDebug = true)).uiState.value,
        )
        assertEquals(
            PaymentHistoryUiState.Error(HistoryFailure.NOT_SET_UP),
            shown(notSetUp, FakeAppConfig(isDebug = false)).uiState.value,
        )
    }

    @Test
    fun unreachableEmulatorsTellTheDeveloperToStartThem() = runTest {
        val offline = FailingHistory(reason = HistoryFailure.NETWORK)
        assertEquals(
            PaymentHistoryUiState.Error(HistoryFailure.NETWORK, SetupHelp.StartEmulators("10.0.2.2")),
            shown(offline, FakeAppConfig(isDebug = true, firebaseEmulatorHost = "10.0.2.2")).uiState.value,
        )
        // Against the real project a network failure is the user's connection.
        assertEquals(
            PaymentHistoryUiState.Error(HistoryFailure.NETWORK),
            shown(offline, FakeAppConfig(isDebug = true)).uiState.value,
        )
        // Other failures keep the user-facing message, emulators or not.
        assertEquals(
            PaymentHistoryUiState.Error(HistoryFailure.UNAVAILABLE),
            shown(FailingHistory(), FakeAppConfig(isDebug = true, firebaseEmulatorHost = "10.0.2.2")).uiState.value,
        )
    }

    @Test
    fun refreshReconnectsTheListener() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = shown(repository)
        repository.history.emit(listOf(testPayment()))

        vm.onEvent(PaymentHistoryEvent.Refresh)

        assertEquals(2, repository.historyCollections)
        assertEquals(listOf(testPayment()), assertIs<PaymentHistoryUiState.Content>(vm.uiState.value).payments)
    }

    @Test
    fun refreshAfterAnErrorShowsLoadingThenRecovers() = runTest {
        val repository = FailingHistory(failures = 1)
        val vm = shown(repository)
        assertEquals(PaymentHistoryUiState.Error(HistoryFailure.UNAVAILABLE), vm.uiState.value)

        vm.onEvent(PaymentHistoryEvent.Refresh)

        assertEquals(PaymentHistoryUiState.Empty, vm.uiState.value)
    }

    @Test
    fun nothingListensUntilTheScreenIsShown() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)
        assertEquals(0, repository.historyCollections)

        show(vm)

        assertEquals(1, repository.history.subscriptionCount.value)
    }

    @Test
    fun theListenerStopsAfterTheScreenIsHiddenAndResumesWhenItReturns() = runTest {
        val repository = FakePaymentRepository(SendPaymentResult.Success(testPayment()))
        val vm = viewModel(repository)
        val screen = show(vm)
        repository.history.emit(listOf(testPayment()))

        screen.cancel()
        advanceTimeBy(HISTORY_STOP_TIMEOUT_MS - 1)
        // A configuration change resubscribes within the timeout without reconnecting.
        assertEquals(1, repository.history.subscriptionCount.value)
        advanceTimeBy(2)
        assertEquals(0, repository.history.subscriptionCount.value, "a hidden screen must not keep listening")
        assertEquals(listOf(testPayment()), assertIs<PaymentHistoryUiState.Content>(vm.uiState.value).payments)

        show(vm)

        assertEquals(1, repository.history.subscriptionCount.value)
        assertEquals(2, repository.historyCollections)
        assertEquals(listOf(testPayment()), assertIs<PaymentHistoryUiState.Content>(vm.uiState.value).payments)
    }

    @Test
    fun refreshKeepsTheListOnScreenWhileReconnecting() = runTest {
        val history = MutableSharedFlow<List<Payment>>()
        val repository = object : PaymentRepository {
            override suspend fun send(request: PaymentRequest, idempotencyKey: String): SendPaymentResult =
                error("Not used")

            override fun observeHistory(): Flow<List<Payment>> = history
        }
        val vm = shown(repository)
        history.emit(listOf(testPayment()))

        vm.onEvent(PaymentHistoryEvent.Refresh)

        val refreshing = assertIs<PaymentHistoryUiState.Content>(vm.uiState.value)
        assertTrue(refreshing.isRefreshing)
        history.emit(listOf(testPayment("pay_2")))
        assertEquals(
            PaymentHistoryUiState.Content(listOf(testPayment("pay_2"))),
            vm.uiState.value,
        )
    }

    private class FailingHistory(
        private var failures: Int = Int.MAX_VALUE,
        reason: HistoryFailure = HistoryFailure.UNAVAILABLE,
        private val failure: Throwable = PaymentHistoryException(reason),
    ) : PaymentRepository {
        override suspend fun send(
            request: PaymentRequest,
            idempotencyKey: String,
        ): SendPaymentResult = error("Not used")

        override fun observeHistory(): Flow<List<Payment>> = flow {
            if (failures-- > 0) throw failure
            emit(emptyList())
        }
    }
}
