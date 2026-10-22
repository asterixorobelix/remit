package com.example.myproject.plugins

import com.example.myproject.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DatabasesTest {
    @Test
    fun `readiness turns false when the pool is closed`() = runBlocking {
        val config = DatabaseConfig.inMemoryH2("ready-${UUID.randomUUID()}")
        val dataSource = HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = config.url
                username = config.user
                maximumPoolSize = 1
            },
        )
        val database = Database.connect(dataSource)
        try {
            assertTrue(database.isReady())
            dataSource.close()
            assertFalse(database.isReady())
        } finally {
            TransactionManager.closeAndUnregister(database)
        }
    }
}
