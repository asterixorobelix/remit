package com.asterixorobelix.remit.data.preferences

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.asterixorobelix.remit.domain.settings.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okio.Path.Companion.toPath

/** Device-local settings. Reads and writes suspend; nothing blocks the main thread. */
interface AppPreferences {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun hasCompletedOnboarding(): Boolean
    suspend fun setOnboardingCompleted(completed: Boolean)

    /** Counts one more launch and returns the new total. */
    suspend fun incrementAppOpenCount(): Int
    suspend fun hasRequestedReview(): Boolean
    suspend fun setReviewRequested()

    // Debug-only overrides. Production UI must never expose these controls.
    suspend fun getApiBaseUrlOverride(): String?
    suspend fun setApiBaseUrlOverride(url: String?)
    suspend fun resetForDevelopment()
}

/** [AppPreferences] in a Preferences DataStore; each platform module supplies the store. */
class DataStoreAppPreferences(private val store: DataStore<Preferences>) : AppPreferences {
    override val themeMode: Flow<ThemeMode> = store.data
        .map { preferences -> ThemeMode.entries.firstOrNull { it.name == preferences[THEME_MODE] } ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    override suspend fun hasCompletedOnboarding(): Boolean = read(ONBOARDING_COMPLETED) ?: false

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        store.edit { it[ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun incrementAppOpenCount(): Int =
        store.edit { it[APP_OPEN_COUNT] = (it[APP_OPEN_COUNT] ?: 0) + 1 }[APP_OPEN_COUNT] ?: 0

    override suspend fun hasRequestedReview(): Boolean = read(REVIEW_REQUESTED) ?: false

    override suspend fun setReviewRequested() {
        store.edit { it[REVIEW_REQUESTED] = true }
    }

    override suspend fun getApiBaseUrlOverride(): String? = read(API_URL_OVERRIDE)

    override suspend fun setApiBaseUrlOverride(url: String?) {
        store.edit { if (url == null) it.remove(API_URL_OVERRIDE) else it[API_URL_OVERRIDE] = url }
    }

    override suspend fun resetForDevelopment() {
        store.edit { it.clear() }
    }

    private suspend fun <T> read(key: Preferences.Key<T>): T? = store.data.first()[key]

    companion object {
        /** The store's file name; each platform puts it in its app-private directory. */
        const val FILE_NAME = "app_settings.preferences_pb"

        // The names and types match the SharedPreferences file this replaced, so Android's
        // SharedPreferencesMigration copies existing values as they are.
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        private val REVIEW_REQUESTED = booleanPreferencesKey("review_requested")
        private val API_URL_OVERRIDE = stringPreferencesKey("api_url_override")
    }
}

/**
 * Opens the settings store at [path]. DataStore allows one active instance per file in a process
 * (until its [scope] is cancelled), so create it once: a Koin `single` in the platform module. A
 * corrupt file is replaced with empty settings rather than failing every read.
 */
fun createPreferencesDataStore(
    path: String,
    migrations: List<DataMigration<Preferences>> = emptyList(),
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    migrations = migrations,
    scope = scope,
    produceFile = { path.toPath() },
)
