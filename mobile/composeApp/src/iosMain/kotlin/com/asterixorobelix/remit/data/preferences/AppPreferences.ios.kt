package com.asterixorobelix.remit.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The settings store in the app's Documents directory. */
@OptIn(ExperimentalForeignApi::class)
fun iosPreferencesDataStore(): DataStore<Preferences> {
    val documents = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    val directory = requireNotNull(documents?.path) { "No Documents directory" }
    return createPreferencesDataStore("$directory/${DataStoreAppPreferences.FILE_NAME}")
}
