package com.asterixorobelix.remit.service

import com.asterixorobelix.remit.configuration.FirebaseConfig
import com.asterixorobelix.remit.domain.Currency
import com.asterixorobelix.remit.domain.PaymentStatus
import com.asterixorobelix.remit.domain.StoredPayment
import com.asterixorobelix.remit.postJson
import com.asterixorobelix.remit.routes.IDEMPOTENCY_KEY_HEADER
import com.asterixorobelix.remit.routes.IDEMPOTENT_REPLAYED_HEADER
import com.asterixorobelix.remit.routes.PaymentsFeature
import com.asterixorobelix.remit.startApp
import com.asterixorobelix.remit.testConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.cloud.FirestoreClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The real Firebase adapters (Admin SDK, Firestore transactions, ID token verification) against
 * the Firebase Emulator Suite. Skipped unless the emulators are running: `make test-backend-emulated`.
 */
class FirebaseEmulatorTest {
    private val firestoreHost: String? = System.getenv("FIRESTORE_EMULATOR_HOST")
    private val authHost: String? = System.getenv("FIREBASE_AUTH_EMULATOR_HOST")
    private val projectId = System.getenv("GCLOUD_PROJECT") ?: "demo-remit-test"

    private val feature: PaymentsFeature by lazy {
        checkNotNull(FirebaseBackend.paymentsFeature(FirebaseConfig(projectId, firestoreHost, authHost)))
    }
    private val firestore by lazy { FirestoreClient.getFirestore(FirebaseApp.getInstance("remit-backend")) }

    @BeforeTest
    fun requireEmulators() {
        assumeTrue(firestoreHost != null && authHost != null, "Firebase emulators are not running")
    }

    @Test
    fun paymentGoesThroughTokenVerificationIntoFirestore() = testApplication {
        startApp(testConfig(), feature)
        val (token, uid) = signUpAnonymously()
        val key = UUID.randomUUID().toString()
        val body = """{"recipientEmail":"Bob@Example.com","amount":10.50,"currency":"EUR"}"""

        val created = client.postJson("/api/v1/payments", body) {
            bearerAuth(token)
            header(IDEMPOTENCY_KEY_HEADER, key)
        }
        val replay = client.postJson("/api/v1/payments", body) {
            bearerAuth(token)
            header(IDEMPOTENCY_KEY_HEADER, key)
        }

        assertEquals(HttpStatusCode.Created, created.status)
        assertNull(created.headers[IDEMPOTENT_REPLAYED_HEADER])
        assertEquals("true", replay.headers[IDEMPOTENT_REPLAYED_HEADER])
        assertEquals(created.bodyAsText(), replay.bodyAsText())
        val id = Json.parseToJsonElement(created.bodyAsText()).jsonObject.getValue("id").jsonPrimitive.content
        val stored = firestore.collection("payments").document(id).get().get()
        assertEquals(uid, stored.getString("senderUid"))
        assertEquals("bob@example.com", stored.getString("recipientEmail"))
        assertEquals(1050L, stored.getLong("amountMinor"))
        assertEquals("EUR", stored.getString("currency"))
    }

    @Test
    fun forgedTokensAreRejected() = runBlocking {
        assertNull(feature.tokens.userId("not-a-token"))
    }

    @Test
    fun concurrentRequestsWithOneKeyCreateOnePayment() = runBlocking {
        val store = FirestorePaymentStore(firestore)
        val key = UUID.randomUUID().toString()
        val outcomes = List(10) { index ->
            async { store.createOnce(key, "fingerprint", payment("pay_${UUID.randomUUID()}", attempt = index)) }
        }.awaitAll()

        assertEquals(1, outcomes.count { it is CreateOutcome.Created })
        val originalId = (outcomes.single { it is CreateOutcome.Created } as CreateOutcome.Created).payment.id
        outcomes.filterIsInstance<CreateOutcome.Replayed>().forEach { assertEquals(originalId, it.payment.id) }
        assertEquals(CreateOutcome.KeyReused, store.createOnce(key, "other", payment("pay_${UUID.randomUUID()}", 0)))
    }

    private fun payment(id: String, attempt: Int) = StoredPayment(
        id = id,
        senderUid = "concurrency-user",
        recipientEmail = "bob@example.com",
        amountMinor = 500L + attempt,
        currency = Currency.USD,
        status = PaymentStatus.COMPLETED,
        createdAt = Instant.parse("2026-10-22T08:30:00Z"),
    )

    /** An anonymous user from the Auth emulator, as the app's first launch creates one. */
    private fun signUpAnonymously(): Pair<String, String> {
        val request = HttpRequest.newBuilder(
            URI("http://$authHost/identitytoolkit.googleapis.com/v1/accounts:signUp?key=fake-api-key"),
        ).header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString("""{"returnSecureToken":true}"""))
            .build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        val json = Json.parseToJsonElement(response.body()).jsonObject
        return json.getValue("idToken").jsonPrimitive.content to json.getValue("localId").jsonPrimitive.content
    }
}
