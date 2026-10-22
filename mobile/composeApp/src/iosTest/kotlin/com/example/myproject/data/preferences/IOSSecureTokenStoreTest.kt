package com.example.myproject.data.preferences

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Round-trips tokens through the real Keychain APIs. Uses a dedicated service name so it never
 * touches the app's own items.
 *
 * Unhosted Kotlin/Native test executables (`iosSimulatorArm64Test`) have no keychain and get
 * errSecNotAvailable (-25291) or errSecMissingEntitlement (-34018); the query is still built and
 * bridged, so these tests then only prove the CoreFoundation bridging does not crash and that reads
 * degrade to null. The full round trip runs whenever a keychain is available.
 */
class IOSSecureTokenStoreTest {
    private val store = IOSSecureTokenStore(service = "com.example.myproject.tests")

    @Test
    fun readsNeverThrowEvenWithoutAKeychain() {
        assertNull(store.get("missing-key"))
    }

    private fun keychainAvailable(): Boolean = runCatching { store.set(PROBE_KEY, "probe") }
        .fold(
            onSuccess = {
                store.set(PROBE_KEY, null)
                true
            },
            onFailure = { error ->
                val message = error.message.orEmpty()
                check("-25291" in message || "-34018" in message) { "Unexpected Keychain failure: $message" }
                println("Keychain unavailable in this test host ($message); skipping round trip")
                false
            },
        )

    @AfterTest
    fun cleanUp() {
        if (keychainAvailable()) store.clear()
    }

    @Test
    fun storesReadsOverwritesAndDeletesTokens() {
        if (!keychainAvailable()) return
        assertNull(store.get(IOSSecureTokenStore.ACCESS_TOKEN_KEY))

        store.set(IOSSecureTokenStore.ACCESS_TOKEN_KEY, "first-token")
        assertEquals("first-token", store.get(IOSSecureTokenStore.ACCESS_TOKEN_KEY))

        store.set(IOSSecureTokenStore.ACCESS_TOKEN_KEY, "rotated-token-ü")
        assertEquals("rotated-token-ü", store.get(IOSSecureTokenStore.ACCESS_TOKEN_KEY))

        store.set(IOSSecureTokenStore.ACCESS_TOKEN_KEY, null)
        assertNull(store.get(IOSSecureTokenStore.ACCESS_TOKEN_KEY))
    }

    @Test
    fun clearRemovesBothTokens() {
        if (!keychainAvailable()) return
        store.set(IOSSecureTokenStore.ACCESS_TOKEN_KEY, "access")
        store.set(IOSSecureTokenStore.REFRESH_TOKEN_KEY, "refresh")
        store.clear()
        assertNull(store.get(IOSSecureTokenStore.ACCESS_TOKEN_KEY))
        assertNull(store.get(IOSSecureTokenStore.REFRESH_TOKEN_KEY))
    }

    private companion object {
        const val PROBE_KEY = "probe"
    }
}
