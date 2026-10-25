package com.asterixorobelix.remit.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences

/**
 * The settings store in the app's private files. The first open copies the values from the
 * SharedPreferences file it replaced (and deletes that file), so an update keeps them.
 */
fun androidPreferencesDataStore(context: Context): DataStore<Preferences> = createPreferencesDataStore(
    path = context.filesDir.resolve("datastore/${DataStoreAppPreferences.FILE_NAME}").absolutePath,
    migrations = listOf(SharedPreferencesMigration(context, LEGACY_SHARED_PREFERENCES)),
)

private const val LEGACY_SHARED_PREFERENCES = "app_preferences"
