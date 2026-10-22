package com.asterixorobelix.remit.data.payments

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.platform.AppDispatchers
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.PaymentRequest
import com.asterixorobelix.remit.shared.domain.repository.PendingPayment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlDelightPendingPaymentStoreTest {
    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
    private val database = AppDatabase(driver)
    private val store = SqlDelightPendingPaymentStore(
        database,
        object : AppDispatchers {
            override val io: CoroutineDispatcher = Dispatchers.Unconfined
        },
    )

    private fun pending(key: String, cents: Long = CENTS, currency: Currency = Currency.USD) = PendingPayment(
        PaymentRequest(requireNotNull(EmailAddress.parse("bob@example.com")), Money(cents, currency)),
        key,
    )

    @Test
    fun startsEmpty() = runTest {
        assertNull(store.load())
    }

    @Test
    fun keepsExactlyOnePendingPayment() = runTest {
        store.save(pending("key-1"))
        store.save(pending("key-2", cents = OTHER_CENTS, currency = Currency.EUR))
        assertEquals(pending("key-2", cents = OTHER_CENTS, currency = Currency.EUR), store.load())
        assertEquals(1L, database.pendingPaymentQueries.get().executeAsList().size.toLong())
    }

    @Test
    fun clearForgetsIt() = runTest {
        store.save(pending("key-1"))
        store.clear()
        assertNull(store.load())
    }

    @Test
    fun aRowThatNoLongerParsesIsDropped() = runTest {
        database.pendingPaymentQueries.save("key-1", "bob@example.com", CENTS, "XXX")
        assertNull(store.load())
        assertNull(database.pendingPaymentQueries.get().executeAsOneOrNull())
    }

    @Test
    fun survivesANewStoreOverTheSameDatabase() = runTest {
        store.save(pending("key-1"))
        val reopened = SqlDelightPendingPaymentStore(
            AppDatabase(driver),
            object : AppDispatchers {
                override val io: CoroutineDispatcher = Dispatchers.Unconfined
            },
        )
        assertEquals("key-1", reopened.load()?.idempotencyKey)
    }

    private companion object {
        const val CENTS = 1050L
        const val OTHER_CENTS = 99L
    }
}
