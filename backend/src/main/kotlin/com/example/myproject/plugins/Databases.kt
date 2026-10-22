package com.example.myproject.plugins

import com.example.myproject.configuration.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.slf4j.LoggerFactory
import java.sql.SQLException
import javax.sql.DataSource

private val logger = LoggerFactory.getLogger("com.example.myproject.plugins.Databases")

/**
 * Starts the Hikari pool, applies Flyway migrations (`resources/db/migration/Vn__*.sql`) before
 * serving traffic, and connects Exposed. The pool is closed when the application stops.
 */
fun Application.configureDatabases(config: DatabaseConfig): Database {
    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.url
            driverClassName = config.driver
            username = config.user
            password = config.password
            maximumPoolSize = config.poolSize
            connectionTimeout = config.connectionTimeoutMs
            idleTimeout = config.idleTimeoutMs
            maxLifetime = config.maxLifetimeMs
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            poolName = "app-db"
        },
    )

    try {
        if (config.runMigrations) {
            runMigrations(dataSource)
        } else {
            log.info("Database migrations disabled via RUN_MIGRATIONS=false")
        }
    } catch (cause: FlywayException) {
        log.error("Database migration failed", cause)
        dataSource.close()
        throw cause
    }

    val database = Database.connect(dataSource)
    monitor.subscribe(ApplicationStopped) {
        TransactionManager.closeAndUnregister(database)
        dataSource.close()
    }
    return database
}

private fun Application.runMigrations(dataSource: DataSource) {
    val result = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .baselineOnMigrate(true)
        .baselineVersion("0")
        .validateMigrationNaming(true)
        .load()
        .migrate()

    if (result.migrationsExecuted > 0) {
        log.info("Applied ${result.migrationsExecuted} migration(s); schema version ${result.targetSchemaVersion}")
    } else {
        log.info("Database schema is up to date")
    }
}

/**
 * Runs [block] in a transaction on [Dispatchers.IO] against this database.
 */
suspend fun <T> Database.dbQuery(block: suspend JdbcTransaction.() -> T): T =
    withContext(Dispatchers.IO) { suspendTransaction(db = this@dbQuery) { block() } }

/**
 * Returns true when the database answers a trivial query.
 */
suspend fun Database.isReady(): Boolean = try {
    dbQuery { exec("SELECT 1") { resultSet -> resultSet.next() } == true }
} catch (cause: SQLException) {
    logger.warn("Database readiness check failed: {}", cause.message)
    false
}
