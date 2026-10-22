package com.asterixorobelix.remit.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FirebaseConfigTest {
    private fun load(environment: Environment, vararg values: Pair<String, String>) =
        FirebaseConfig.fromEnv(mapOf(*values)::get, environment)

    @Test
    fun readsProjectAndEmulatorHosts() {
        val config = load(
            Environment.DEVELOPMENT,
            "FIREBASE_PROJECT_ID" to " remit-payments ",
            "FIRESTORE_EMULATOR_HOST" to "localhost:8080",
        )
        assertEquals("remit-payments", config.projectId)
        assertTrue(config.usesEmulators)
    }

    @Test
    fun developmentWithoutAProjectDisablesPayments() {
        val config = load(Environment.DEVELOPMENT, "FIREBASE_PROJECT_ID" to " ")
        assertEquals(null, config.projectId)
        assertFalse(config.usesEmulators)
    }

    @Test
    fun productionFailsClosed() {
        assertFailsWith<IllegalArgumentException> { load(Environment.PRODUCTION) }
        assertFailsWith<IllegalArgumentException> {
            load(
                Environment.PRODUCTION,
                "FIREBASE_PROJECT_ID" to "p",
                "FIREBASE_AUTH_EMULATOR_HOST" to "localhost:9099",
            )
        }
        assertEquals("p", load(Environment.PRODUCTION, "FIREBASE_PROJECT_ID" to "p").projectId)
    }
}
