package com.asterixorobelix.remit.data.preferences

import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.serialization.json.Json
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSLog
import platform.Foundation.NSUserDefaults
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData
import platform.posix.memcpy

private const val MAX_RECENT_SEARCHES = 10

class IOSAppPreferences(
    private val secureTokenStore: IOSSecureTokenStore = IOSSecureTokenStore(),
) : AppPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    init {
        migrateLegacyToken(KEY_ACCESS_TOKEN)
        migrateLegacyToken(KEY_REFRESH_TOKEN)
    }

    override fun hasCompletedOnboarding(): Boolean =
        defaults.boolForKey(KEY_ONBOARDING_COMPLETED)

    override fun setOnboardingCompleted(completed: Boolean) {
        defaults.setBool(completed, KEY_ONBOARDING_COMPLETED)
    }

    override fun getThemeMode(): ThemeMode {
        val value = defaults.stringForKey(KEY_THEME_MODE)
        return ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
    }

    override fun setThemeMode(mode: ThemeMode) {
        defaults.setObject(mode.name, KEY_THEME_MODE)
    }

    override fun getAppOpenCount(): Int =
        defaults.integerForKey(KEY_APP_OPEN_COUNT).toInt()

    override fun incrementAppOpenCount() {
        val current = getAppOpenCount()
        defaults.setInteger(current.toLong() + 1, KEY_APP_OPEN_COUNT)
    }

    override fun hasRequestedReview(): Boolean =
        defaults.boolForKey(KEY_REVIEW_REQUESTED)

    override fun setReviewRequested() {
        defaults.setBool(true, KEY_REVIEW_REQUESTED)
    }

    override fun getAccessToken(): String? = secureTokenStore.get(KEY_ACCESS_TOKEN)
    override fun setAccessToken(token: String?) {
        secureTokenStore.set(KEY_ACCESS_TOKEN, token)
    }

    override fun getRefreshToken(): String? = secureTokenStore.get(KEY_REFRESH_TOKEN)
    override fun setRefreshToken(token: String?) {
        secureTokenStore.set(KEY_REFRESH_TOKEN, token)
    }

    override fun clearAuthTokens() {
        secureTokenStore.clear()
    }

    override fun getUserDisplayName(): String? = defaults.stringForKey(KEY_USER_DISPLAY_NAME)
    override fun setUserDisplayName(name: String?) {
        setOrRemove(KEY_USER_DISPLAY_NAME, name)
    }

    override fun getUserEmail(): String? = defaults.stringForKey(KEY_USER_EMAIL)
    override fun setUserEmail(email: String?) {
        setOrRemove(KEY_USER_EMAIL, email)
    }

    override fun getRecentSearches(): List<String> {
        val json = defaults.stringForKey(KEY_RECENT_SEARCHES) ?: return emptyList()
        return Json.decodeFromString<List<String>>(json)
    }

    override fun addRecentSearch(query: String) {
        val searches = getRecentSearches().toMutableList()
        searches.remove(query)
        searches.add(0, query)
        val trimmed = searches.take(MAX_RECENT_SEARCHES)
        defaults.setObject(Json.encodeToString(trimmed), KEY_RECENT_SEARCHES)
    }

    override fun clearRecentSearches() {
        defaults.removeObjectForKey(KEY_RECENT_SEARCHES)
    }
    override fun getApiBaseUrlOverride(): String? = defaults.stringForKey(KEY_API_OVERRIDE)
    override fun setApiBaseUrlOverride(url: String?) {
        setOrRemove(KEY_API_OVERRIDE, url)
    }
    override fun resetForDevelopment() {
        clearAuthTokens()
        listOf(
            KEY_ONBOARDING_COMPLETED,
            KEY_THEME_MODE,
            KEY_APP_OPEN_COUNT,
            KEY_REVIEW_REQUESTED,
            KEY_USER_DISPLAY_NAME,
            KEY_USER_EMAIL,
            KEY_RECENT_SEARCHES,
            KEY_API_OVERRIDE,
        ).forEach(defaults::removeObjectForKey)
    }

    private fun setOrRemove(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, key)
    }

    private fun migrateLegacyToken(key: String) {
        val legacyToken = defaults.stringForKey(key) ?: return
        if (secureTokenStore.get(key) == null) {
            secureTokenStore.set(key, legacyToken)
        }
        defaults.removeObjectForKey(key)
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_APP_OPEN_COUNT = "app_open_count"
        private const val KEY_REVIEW_REQUESTED = "review_requested"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_DISPLAY_NAME = "user_display_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_RECENT_SEARCHES = "recent_searches"
        private const val KEY_API_OVERRIDE = "api_url_override"
    }
}

actual fun createAppPreferences(): AppPreferences = IOSAppPreferences()

/**
 * Generic-password Keychain storage for auth tokens.
 *
 * Queries are built as real CoreFoundation dictionaries (CFDictionaryCreateMutable with the CFType
 * callbacks) and Kotlin values are bridged with CFBridgingRetain/CFBridgingRelease. Items are
 * `AfterFirstUnlockThisDeviceOnly`: readable by background refreshes, never restored to another
 * device from a backup.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IOSSecureTokenStore(private val service: String = KEYCHAIN_SERVICE) {

    /**
     * Returns the stored value, or null when absent. Read failures (for example
     * errSecInteractionNotAllowed before first unlock, or a build without keychain entitlements)
     * are logged and treated as "no session" instead of crashing the app.
     */
    fun get(key: String): String? = withQuery(key, returnData = true) { query ->
        memScoped {
            val result = alloc<CFTypeRefVar>()
            when (val status = SecItemCopyMatching(query, result.ptr)) {
                errSecSuccess -> (CFBridgingRelease(result.value) as? NSData)?.toByteArray()?.decodeToString()

                errSecItemNotFound -> null

                else -> {
                    NSLog("Keychain read failed for $key (OSStatus $status)")
                    null
                }
            }
        }
    }

    fun set(key: String, value: String?) {
        withQuery(key) { query -> SecItemDelete(query) }
        if (value == null) return
        val status = withQuery(key, data = value.encodeToByteArray().toNSData()) { query -> SecItemAdd(query, null) }
        check(status == errSecSuccess) { "Unable to save authentication token to Keychain (OSStatus $status)" }
    }

    fun clear() {
        set(ACCESS_TOKEN_KEY, null)
        set(REFRESH_TOKEN_KEY, null)
    }

    private fun <T> withQuery(
        key: String,
        returnData: Boolean = false,
        data: NSData? = null,
        block: (CFDictionaryRef?) -> T,
    ): T {
        val query = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr,
        )
        // Bridged objects are released after the call; the dictionary retains what it needs.
        val bridged = listOfNotNull(
            CFBridgingRetain(service)?.also { CFDictionaryAddValue(query, kSecAttrService, it) },
            CFBridgingRetain(key)?.also { CFDictionaryAddValue(query, kSecAttrAccount, it) },
            data?.let { CFBridgingRetain(it) }?.also { CFDictionaryAddValue(query, kSecValueData, it) },
        )
        CFDictionaryAddValue(query, kSecClass, kSecClassGenericPassword)
        if (returnData) {
            CFDictionaryAddValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionaryAddValue(query, kSecMatchLimit, kSecMatchLimitOne)
        }
        if (data != null) {
            CFDictionaryAddValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
        }
        return try {
            block(query)
        } finally {
            bridged.forEach(::CFRelease)
            CFRelease(query)
        }
    }

    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
        }
        return bytes
    }

    companion object {
        const val KEYCHAIN_SERVICE = "com.asterixorobelix.remit.auth"
        const val ACCESS_TOKEN_KEY = "access_token"
        const val REFRESH_TOKEN_KEY = "refresh_token"
    }
}
