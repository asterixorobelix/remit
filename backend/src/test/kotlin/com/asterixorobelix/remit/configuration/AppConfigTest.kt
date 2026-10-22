package com.asterixorobelix.remit.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {
    private fun load(env: Map<String, String>) = AppConfig.load { env[it] }

    @Test
    fun `environment fails closed when unset`() {
        assertEquals(Environment.PRODUCTION, Environment.current { null })
        assertEquals(Environment.PRODUCTION, Environment.current { if (it == "APP_ENVIRONMENT") " " else null })
        assertEquals(Environment.DEVELOPMENT, Environment.current { if (it == "APP_ENVIRONMENT") "dev" else null })
        assertEquals(Environment.STAGING, Environment.current { if (it == "ENVIRONMENT") "qa" else null })
        assertFailsWith<IllegalArgumentException> { Environment.fromString("prodution") }
    }

    @Test
    fun `unset environment loads secure production defaults`() {
        // Production also requires a Firebase project; without one the server refuses to start.
        assertFailsWith<IllegalArgumentException> { load(emptyMap()) }
        val config = load(mapOf("FIREBASE_PROJECT_ID" to "remit-payments"))
        assertEquals(Environment.PRODUCTION, config.environment)
        assertEquals("remit-payments", config.firebase.projectId)
        assertEquals(emptyList(), config.cors.allowedHosts)
        assertFalse(config.cors.allowCredentials)
        assertFalse(config.features.swaggerEnabled)
        assertEquals(ServerConfig.DEFAULT_TRUSTED_PROXY_HOPS, config.server.trustedProxyHops)
        assertEquals(RemoteConfig(), config.remoteConfig)
    }

    @Test
    fun `staging keeps development CORS origins off`() {
        val config = load(mapOf("APP_ENVIRONMENT" to "staging"))
        assertEquals(Environment.STAGING, config.environment)
        assertEquals(emptyList(), config.cors.allowedHosts)
        assertTrue(config.features.swaggerEnabled)
    }

    @Test
    fun `development uses local defaults`() {
        val config = load(mapOf("APP_ENVIRONMENT" to "development"))
        assertEquals(CorsConfig.DEVELOPMENT_HOSTS, config.cors.allowedHosts)
        assertTrue(config.features.swaggerEnabled)
        assertEquals(ServerConfig.DEFAULT_PORT, config.server.port)
        assertEquals(ServerConfig.DEFAULT_HOST, config.server.host)
    }

    @Test
    fun `explicit variables override defaults`() {
        val config = load(
            mapOf(
                "APP_ENVIRONMENT" to "production",
                "FIREBASE_PROJECT_ID" to "remit-payments",
                "PORT" to "9090",
                "HOST" to "127.0.0.1",
                "SHUTDOWN_GRACE_PERIOD_MS" to "250",
                "SHUTDOWN_TIMEOUT_MS" to "not-a-number",
                "TRUSTED_PROXY_HOPS" to "-3",
                "CORS_ALLOWED_HOSTS" to " https://app.example.com , ,admin.example.com ",
                "CORS_ALLOW_CREDENTIALS" to "true",
                "SWAGGER_ENABLED" to "true",
                "MIN_APP_VERSION" to "1.2.0",
                "LATEST_APP_VERSION" to "1.3.0",
                "FORCE_UPDATE" to "yes",
                "MAINTENANCE_MODE" to "true",
            ),
        )
        assertEquals(9090, config.server.port)
        assertEquals("127.0.0.1", config.server.host)
        assertEquals(250, config.server.shutdownGracePeriodMs)
        assertEquals(ServerConfig.DEFAULT_SHUTDOWN_TIMEOUT, config.server.shutdownTimeoutMs)
        assertEquals(0, config.server.trustedProxyHops)
        assertEquals(listOf("https://app.example.com", "admin.example.com"), config.cors.allowedHosts)
        assertTrue(config.cors.allowCredentials)
        assertTrue(config.features.swaggerEnabled)
        assertEquals(RemoteConfig("1.2.0", "1.3.0", forceUpdate = false, maintenanceMode = true), config.remoteConfig)
    }

    @Test
    fun `build info falls back when the resource is missing`() {
        assertEquals(BuildInfo.UNKNOWN_VERSION, BuildInfo.readVersion("/missing.properties"))
        assertTrue(BuildInfo.version.matches(Regex("\\d+\\.\\d+\\.\\d+.*")), BuildInfo.version)
    }
}
