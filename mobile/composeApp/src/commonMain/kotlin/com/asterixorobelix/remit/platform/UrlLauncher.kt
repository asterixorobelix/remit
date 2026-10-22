package com.asterixorobelix.remit.platform

/**
 * Opens [url] with the platform handler (browser, mail client, store app).
 *
 * Supported schemes: `https`, `http`, `mailto`, plus the platform store scheme
 * (`market` on Android, `itms-apps` on iOS). Returns `false` when the scheme is not
 * allowed or no installed app can handle the URL, so callers can show feedback.
 */
expect fun openUrl(url: String): Boolean

/** Identifiers used to open this app's store listing. */
data class StoreListing(
    /** Android application ID (release package name, without build-type suffixes). */
    val androidPackage: String,
    /** Numeric App Store Connect app ID (the digits after `id` in the App Store URL). */
    val appStoreId: String,
)

/** Opens the store listing for this app. Returns `false` if nothing could be opened. */
expect fun openStoreListing(listing: StoreListing): Boolean
