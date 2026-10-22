package com.example.myproject.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {
    private val productionEnv = mapOf(
        "APP_ENVIRONMENT" to "production",
        "DATABASE_URL" to "postgresql://app:s3cret%40pw@db.example.com:5432/app?sslmode=require",
        "JWT_SECRET" to "test-production-secret-0123456789abcdefghij",
        "JWT_ISSUER" to "https://api.example.com",
        "JWT_AUDIENCE" to "https://api.example.com",
    )

    private fun load(env: Map<String, String>) = AppConfig.load { env[it] }

    @Test
    fun `provider postgres URL is normalized to JDBC`() {
        assertEquals(
            "jdbc:postgresql://db.example.com:5432/app?sslmode=require",
            DatabaseConfig.normalizeDatabaseUrl("postgresql://user:password@db.example.com:5432/app?sslmode=require"),
        )
        assertEquals(
            "jdbc:postgresql://db.example.com/app",
            DatabaseConfig.normalizeDatabaseUrl("postgres://user:password@db.example.com/app"),
        )
    }

    @Test
    fun `H2 URLs always use PostgreSQL compatibility mode`() {
        assertEquals(
            "jdbc:h2:file:./data/app;${DatabaseConfig.H2_COMPATIBILITY_OPTIONS}",
            DatabaseConfig.normalizeDatabaseUrl("jdbc:h2:file:./data/app"),
        )
        val explicit = "jdbc:h2:mem:x;MODE=MySQL"
        assertEquals(explicit, DatabaseConfig.normalizeDatabaseUrl(explicit))
    }

    @Test
    fun `environment fails closed when unset`() {
        assertEquals(Environment.PRODUCTION, Environment.current { null })
        assertEquals(Environment.PRODUCTION, Environment.current { if (it == "APP_ENVIRONMENT") " " else null })
        assertEquals(Environment.DEVELOPMENT, Environment.current { if (it == "APP_ENVIRONMENT") "dev" else null })
        assertEquals(Environment.STAGING, Environment.current { if (it == "ENVIRONMENT") "qa" else null })
        assertFailsWith<IllegalArgumentException> { Environment.fromString("prodution") }
    }

    @Test
    fun `unset environment refuses to start with development defaults`() {
        val failure = assertFailsWith<IllegalStateException> { load(emptyMap()) }
        val message = failure.message.orEmpty()
        assertTrue(message.contains("DATABASE_URL must point to PostgreSQL"), message)
        assertTrue(message.contains("JWT_SECRET must be a generated secret"), message)
        assertTrue(message.contains("must not use localhost"), message)
        assertTrue(message.contains("APP_ENVIRONMENT=development"), message)
    }

    @Test
    fun `production rejects placeholder secrets of any length`() {
        listOf(
            "CHANGE_ME_GENERATE_WITH_openssl_rand_base64_32",
            "please-change-this-secret-before-deploying-it",
            JwtConfig.DEFAULT_SECRET,
        ).forEach { secret ->
            val failure = assertFailsWith<IllegalStateException> { load(productionEnv + ("JWT_SECRET" to secret)) }
            assertTrue(failure.message.orEmpty().contains("placeholder"), secret)
        }
        val short = assertFailsWith<IllegalStateException> { load(productionEnv + ("JWT_SECRET" to "tooShort")) }
        assertTrue(short.message.orEmpty().contains("at least 32 characters"))
    }

    @Test
    fun `valid production configuration loads with secure defaults`() {
        val config = load(productionEnv)
        assertEquals(Environment.PRODUCTION, config.environment)
        assertTrue(config.database.isPostgres)
        assertEquals(DatabaseConfig.POSTGRES_DRIVER, config.database.driver)
        assertEquals("app", config.database.user)
        assertEquals("s3cret@pw", config.database.password)
        assertEquals(emptyList(), config.cors.allowedHosts)
        assertFalse(config.cors.allowCredentials)
        assertFalse(config.features.swaggerEnabled)
        assertTrue(config.rateLimit.enabled)
        assertEquals(ServerConfig.DEFAULT_TRUSTED_PROXY_HOPS, config.server.trustedProxyHops)
    }

    @Test
    fun `staging is validated like production`() {
        assertFailsWith<IllegalStateException> { load(mapOf("APP_ENVIRONMENT" to "staging")) }
        assertEquals(Environment.STAGING, load(productionEnv + ("APP_ENVIRONMENT" to "staging")).environment)
    }

    @Test
    fun `development uses local defaults`() {
        val config = load(mapOf("APP_ENVIRONMENT" to "development"))
        assertTrue(config.database.isH2)
        assertEquals(DatabaseConfig.DEFAULT_URL, config.database.url)
        assertEquals(JwtConfig.DEFAULT_SECRET, config.jwt.secret)
        assertEquals(CorsConfig.DEVELOPMENT_HOSTS, config.cors.allowedHosts)
        assertTrue(config.features.swaggerEnabled)
        assertEquals(ServerConfig.DEFAULT_PORT, config.server.port)
    }

    @Test
    fun `explicit variables override defaults`() {
        val config = load(
            productionEnv + mapOf(
                "PORT" to "9090",
                "HOST" to "127.0.0.1",
                "TRUSTED_PROXY_HOPS" to "-3",
                "DATABASE_USER" to "override",
                "DB_POOL_SIZE" to "3",
                "RUN_MIGRATIONS" to "FALSE",
                "JWT_EXPIRATION_MS" to "60000",
                "JWT_REFRESH_EXPIRATION_MS" to "120000",
                "CORS_ALLOWED_HOSTS" to " https://app.example.com , ,admin.example.com ",
                "CORS_ALLOW_CREDENTIALS" to "true",
                "SWAGGER_ENABLED" to "true",
                "RATE_LIMITING_ENABLED" to "false",
                "RATE_LIMIT_AUTH_PER_MINUTE" to "5",
                "RATE_LIMIT_REFRESH_PER_MINUTE" to "not-a-number",
                "MIN_APP_VERSION" to "1.2.0",
                "LATEST_APP_VERSION" to "1.3.0",
                "FORCE_UPDATE" to "yes",
                "MAINTENANCE_MODE" to "true",
            ),
        )
        assertEquals(9090, config.server.port)
        assertEquals("127.0.0.1", config.server.host)
        assertEquals(0, config.server.trustedProxyHops)
        assertEquals("override", config.database.user)
        assertEquals(3, config.database.poolSize)
        assertFalse(config.database.runMigrations)
        assertEquals(60_000, config.jwt.expirationMs)
        assertEquals(120_000, config.jwt.refreshExpirationMs)
        assertEquals(listOf("https://app.example.com", "admin.example.com"), config.cors.allowedHosts)
        assertTrue(config.cors.allowCredentials)
        assertTrue(config.features.swaggerEnabled)
        assertEquals(RateLimitConfig(false, 5, RateLimitConfig.DEFAULT_REFRESH_PER_MINUTE), config.rateLimit)
        assertEquals(RemoteConfig("1.2.0", "1.3.0", forceUpdate = false, maintenanceMode = true), config.remoteConfig)
    }

    @Test
    fun `build info falls back when the resource is missing`() {
        assertEquals(BuildInfo.UNKNOWN_VERSION, BuildInfo.readVersion("/missing.properties"))
        assertTrue(BuildInfo.version.matches(Regex("\\d+\\.\\d+\\.\\d+.*")), BuildInfo.version)
    }
}
