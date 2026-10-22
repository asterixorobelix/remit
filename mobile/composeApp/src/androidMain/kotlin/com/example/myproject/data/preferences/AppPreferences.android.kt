package com.example.myproject.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import com.example.myproject.domain.settings.ThemeMode
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private const val MAX_RECENT_SEARCHES = 10

class AndroidAppPreferences(
    private val prefs: SharedPreferences,
    private val secureTokenStore: AndroidSecureTokenStore,
) : AppPreferences {

    init {
        migrateLegacyToken(KEY_ACCESS_TOKEN)
        migrateLegacyToken(KEY_REFRESH_TOKEN)
    }

    override fun hasCompletedOnboarding(): Boolean =
        prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    override fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETED, completed) }
    }

    override fun getThemeMode(): ThemeMode {
        val value = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME_MODE, mode.name) }
    }

    override fun getAppOpenCount(): Int =
        prefs.getInt(KEY_APP_OPEN_COUNT, 0)

    override fun incrementAppOpenCount() {
        val current = getAppOpenCount()
        prefs.edit { putInt(KEY_APP_OPEN_COUNT, current + 1) }
    }

    override fun hasRequestedReview(): Boolean =
        prefs.getBoolean(KEY_REVIEW_REQUESTED, false)

    override fun setReviewRequested() {
        prefs.edit { putBoolean(KEY_REVIEW_REQUESTED, true) }
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

    override fun getUserDisplayName(): String? = prefs.getString(KEY_USER_DISPLAY_NAME, null)
    override fun setUserDisplayName(name: String?) {
        prefs.edit { putString(KEY_USER_DISPLAY_NAME, name) }
    }

    override fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    override fun setUserEmail(email: String?) {
        prefs.edit { putString(KEY_USER_EMAIL, email) }
    }

    override fun getRecentSearches(): List<String> {
        val json = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return emptyList()
        return Json.decodeFromString<List<String>>(json)
    }

    override fun addRecentSearch(query: String) {
        val searches = getRecentSearches().toMutableList()
        searches.remove(query)
        searches.add(0, query)
        val trimmed = searches.take(MAX_RECENT_SEARCHES)
        prefs.edit { putString(KEY_RECENT_SEARCHES, Json.encodeToString(trimmed)) }
    }

    override fun clearRecentSearches() {
        prefs.edit { remove(KEY_RECENT_SEARCHES) }
    }
    override fun getApiBaseUrlOverride(): String? = prefs.getString(KEY_API_OVERRIDE, null)
    override fun setApiBaseUrlOverride(url: String?) {
        prefs.edit { putString(KEY_API_OVERRIDE, url) }
    }
    override fun resetForDevelopment() {
        clearAuthTokens()
        prefs.edit { clear() }
    }

    private fun migrateLegacyToken(key: String) {
        val legacyToken = prefs.getString(key, null) ?: return
        if (secureTokenStore.get(key) == null) {
            secureTokenStore.set(key, legacyToken)
        }
        prefs.edit { remove(key) }
    }

    companion object {
        private const val PREFS_NAME = "app_preferences"
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

        private lateinit var instance: SharedPreferences
        private lateinit var applicationContext: Context

        fun init(context: Context) {
            applicationContext = context.applicationContext
            instance = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        fun getSharedPreferences(): SharedPreferences = instance
        fun getApplicationContext(): Context = applicationContext
    }
}

actual fun createAppPreferences(): AppPreferences =
    AndroidAppPreferences(
        prefs = AndroidAppPreferences.getSharedPreferences(),
        secureTokenStore = AndroidSecureTokenStore(AndroidAppPreferences.getApplicationContext()),
    )

class AndroidSecureTokenStore(context: Context) {
    private val encryptedPreferences =
        context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    fun get(key: String): String? {
        val encoded = encryptedPreferences.getString(key, null) ?: return null
        return runCatching { decrypt(encoded) }
            .onFailure { encryptedPreferences.edit { remove(key) } }
            .getOrNull()
    }

    fun set(key: String, value: String?) {
        if (value == null) {
            encryptedPreferences.edit { remove(key) }
        } else {
            encryptedPreferences.edit { putString(key, encrypt(value)) }
        }
    }

    fun clear() {
        encryptedPreferences.edit { clear() }
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val ciphertext = Base64.encodeToString(
            cipher.doFinal(value.encodeToByteArray()),
            Base64.NO_WRAP,
        )
        return "$iv.$ciphertext"
    }

    private fun decrypt(value: String): String {
        val (iv, ciphertext) = value.split('.', limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, Base64.decode(iv, Base64.NO_WRAP)),
        )
        return cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)).decodeToString()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val SECURE_PREFS_NAME = "secure_auth_tokens"
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "myproject_auth_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val GCM_TAG_LENGTH_BITS = 128
    }
}
