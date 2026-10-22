package com.asterixorobelix.remit.platform

import com.asterixorobelix.remit.testing.FakeAppConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppConfigTest {
    @Test
    fun capabilitiesAreCaseInsensitiveAndIgnoreUnknownValues() {
        assertEquals(setOf(Capability.BACKEND, Capability.FIREBASE), parseCapabilities(" Backend ,FIREBASE"))
        assertEquals(setOf(Capability.BACKEND), parseCapabilities("minimal,backend,auth,unknown,"))
        assertEquals(emptySet(), parseCapabilities(null))
    }

    @Test
    fun hasChecksTheCapabilitySet() {
        val config = FakeAppConfig(capabilities = setOf(Capability.FIREBASE))
        assertTrue(config.has(Capability.FIREBASE))
        assertFalse(config.has(Capability.BACKEND))
    }

    @Test
    fun environmentAliasesAndDefault() {
        assertEquals(Environment.PRODUCTION, Environment.fromString("release"))
        assertEquals(Environment.PRODUCTION, Environment.fromString("Prod"))
        assertEquals(Environment.STAGING, Environment.fromString("qa"))
        assertEquals(Environment.DEVELOPMENT, Environment.fromString("anything"))
        assertEquals(Environment.DEVELOPMENT, Environment.fromString(null))
    }

    @Test
    fun telemetryAndVerboseLoggingFollowTheEnvironment() {
        val development = FakeAppConfig(environment = Environment.DEVELOPMENT)
        assertFalse(development.analyticsEnabled)
        assertFalse(development.crashReportingEnabled)
        assertTrue(development.verboseLoggingEnabled)

        val production = FakeAppConfig(environment = Environment.PRODUCTION)
        assertTrue(production.analyticsEnabled)
        assertTrue(production.crashReportingEnabled)
        assertFalse(production.verboseLoggingEnabled)
    }
}
