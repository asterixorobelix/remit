package com.asterixorobelix.remit.shared.bdd

import com.asterixorobelix.remit.shared.data.PaymentRepositoryImpl
import com.asterixorobelix.remit.shared.data.remote.ApiEndpoint
import com.asterixorobelix.remit.shared.data.remote.KtorPaymentsApi
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.DecimalAmount
import com.asterixorobelix.remit.shared.domain.model.DecimalParseResult
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.repository.InMemoryPendingPaymentStore
import com.asterixorobelix.remit.shared.domain.repository.PaymentFailure
import com.asterixorobelix.remit.shared.domain.repository.SendPaymentResult
import com.asterixorobelix.remit.shared.domain.usecase.IdempotencyKeys
import com.asterixorobelix.remit.shared.domain.usecase.SendPaymentUseCase
import com.asterixorobelix.remit.shared.domain.validation.PaymentDraft
import com.asterixorobelix.remit.shared.domain.validation.PaymentValidator
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Steps for the scenarios in resources/features. The real use case, validator, idempotency keys, repository, and
 * Ktor client run against [FakePaymentsBackend] and [FakeFirestore]; only the network and the
 * database are simulated. Cucumber creates a new instance for every scenario.
 */
class PaymentSteps {
    private val firestore = FakeFirestore()
    private val backend = FakePaymentsBackend(firestore)
    private val auth = TestAuthSession()
    private val repository = PaymentRepositoryImpl(
        api = KtorPaymentsApi(
            HttpClient(MockEngine { request -> backend.handle(this, request) }),
            ApiEndpoint { "https://api.remit.test" },
        ),
        history = firestore,
        auth = auth,
    )
    private val pendingStore = InMemoryPendingPaymentStore()
    private var sendPayment = newSession()
    private val viewing = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var draft: PaymentDraft? = null
    private var result: SendPaymentResult? = null
    private var history: List<Payment> = emptyList()
    private var liveHistory: StateFlow<List<Payment>?>? = null

    /** What the app builds on launch; the pending payment store is the device's disk. */
    private fun newSession() = SendPaymentUseCase(
        PaymentValidator(),
        repository,
        IdempotencyKeys(pendingStore) { UUID.randomUUID().toString() },
    )

    @After
    fun stopViewing() = viewing.cancel()

    @When("the app is closed and reopened")
    fun restartApp() {
        sendPayment = newSession()
    }

    @When("I send the unfinished payment again")
    fun resendUnfinished() {
        val unfinished = assertNotNull(runBlocking { sendPayment.unfinishedPayment() }, "No unfinished payment")
        val request = unfinished.request
        draft = PaymentDraft(request.recipient.value, request.amount.toDecimalString(), request.amount.currency.code)
        submit()
    }

    @Given("I am signed in")
    fun signedIn() = signedInAs("user-a")

    @Given("I am signed in as {string}")
    fun signedInAs(uid: String) {
        auth.uid = uid
    }

    @Given("I enter a payment of {string} {string} to {string}")
    fun enterPayment(amount: String, currency: String, recipient: String) {
        draft = PaymentDraft(recipient, amount, currency)
    }

    @Given("the payments API times out once after processing the payment")
    fun timeoutOnce() {
        backend.timeoutAfterProcessingOnce = true
    }

    @Given("I have sent {string} {string} to {string}")
    fun haveSent(amount: String, currency: String, recipient: String) = runBlocking {
        enterPayment(amount, currency, recipient)
        submit()
        assertIs<SendPaymentResult.Success>(result)
        sendPayment.startNewPayment()
    }

    @Given("user {string} has sent {string} {string} to {string}")
    fun otherUserHasSent(uid: String, amount: String, currency: String, recipient: String) {
        firestore.record(uid, recipient, minorUnits(amount, currency), currency)
    }

    @When("I submit the payment")
    @When("I submit the payment again")
    fun submit() {
        result = runBlocking { sendPayment(checkNotNull(draft) { "No payment entered" }) }
    }

    @Then("the payment is accepted")
    fun accepted() {
        assertIs<SendPaymentResult.Success>(result)
    }

    @Then("the payment is rejected with {string}")
    fun rejected(code: String) {
        assertEquals(SendPaymentResult.Invalid(setOf(ValidationError.valueOf(code))), result)
    }

    @Then("the payment fails with {string}")
    fun failed(reason: String) {
        assertEquals(SendPaymentResult.Failed(PaymentFailure.valueOf(reason)), result)
    }

    @Then("nothing is sent to the payments API")
    fun nothingSent() {
        assertTrue(backend.requests.isEmpty(), "Expected no API calls, got ${backend.requests}")
    }

    @Then("the payments API received {string} {string} for {string} with an idempotency key")
    fun apiReceived(amount: String, currency: String, recipient: String) {
        val (key, body) = backend.requests.single()
        assertEquals(amount, body.amount, "The amount must travel as the exact decimal text")
        assertEquals(currency, body.currency)
        assertEquals(recipient, body.recipientEmail)
        assertTrue(key.isNotBlank())
    }

    @Then("Firestore has a payment of {int} cents in {string} to {string} from me")
    fun firestoreHas(cents: Int, currency: String, recipient: String) {
        val stored = firestore.all.single()
        assertEquals(auth.uid, stored.senderUid)
        assertEquals(cents.toLong(), stored.record.amountMinor)
        assertEquals(currency, stored.record.currency)
        assertEquals(recipient, stored.record.recipientEmail)
    }

    @Then("exactly {int} payment(s) to {string} is/are recorded")
    fun recordedCount(count: Int, recipient: String) {
        assertEquals(count, firestore.all.count { it.record.recipientEmail == recipient })
    }

    @When("I open my transaction history")
    fun openHistory() {
        history = runBlocking { withTimeout(WAIT) { repository.observeHistory().first() } }
    }

    @Given("I am viewing my transaction history")
    fun viewHistory() {
        val live = repository.observeHistory().stateIn(viewing, SharingStarted.Eagerly, null)
        runBlocking { withTimeout(WAIT) { live.filterNotNull().first() } }
        liveHistory = live
    }

    @When("a payment of {string} {string} to {string} is recorded for me")
    fun recordedForMe(amount: String, currency: String, recipient: String) {
        firestore.record(auth.uid, recipient, minorUnits(amount, currency), currency)
    }

    @Then("I see {int} payment(s)")
    fun seePayments(count: Int) {
        assertEquals(count, history.size)
    }

    @Then("the newest payment is {string} {string} to {string}")
    fun newest(amount: String, currency: String, recipient: String) {
        val newest = history.first()
        assertEquals(amount, newest.amount.toDecimalString())
        assertEquals(currency, newest.amount.currency.code)
        assertEquals(recipient, newest.recipient.value)
    }

    @Then("my history shows a payment to {string} without refreshing")
    fun liveUpdate(recipient: String) {
        val live = assertNotNull(liveHistory, "Not viewing history")
        runBlocking {
            withTimeout(WAIT) { live.first { payments -> payments?.any { it.recipient.value == recipient } == true } }
        }
    }

    private fun minorUnits(amount: String, currency: String): Long {
        val decimal = assertIs<DecimalParseResult.Parsed>(DecimalAmount.parse(amount)).amount
        return assertNotNull(decimal.toMoney(assertNotNull(Currency.fromCode(currency)))).minorUnits
    }

    private companion object {
        val WAIT = 5.seconds
    }
}
