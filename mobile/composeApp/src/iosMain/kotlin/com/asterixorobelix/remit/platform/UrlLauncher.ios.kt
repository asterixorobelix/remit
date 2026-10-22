package com.asterixorobelix.remit.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

private val ALLOWED_SCHEMES = setOf("https", "http", "mailto", "itms-apps")

/**
 * Opens [url] with the system handler. `canOpenURL` requires non-web schemes to be listed in
 * `LSApplicationQueriesSchemes` (Info.plist declares `mailto` and `itms-apps`); it returns false
 * when, for example, no mail account is configured (always the case on the simulator).
 */
actual fun openUrl(url: String): Boolean {
    val nsUrl = NSURL.URLWithString(url)?.takeIf { it.scheme?.lowercase() in ALLOWED_SCHEMES } ?: return false
    val application = UIApplication.sharedApplication
    val canOpen = application.canOpenURL(nsUrl)
    if (canOpen) application.openURL(nsUrl, options = emptyMap<Any?, Any?>(), completionHandler = null)
    return canOpen
}

/** App Store app first, then the web listing. */
actual fun openStoreListing(listing: StoreListing): Boolean =
    openUrl("itms-apps://apps.apple.com/app/id${listing.appStoreId}") ||
        openUrl("https://apps.apple.com/app/id${listing.appStoreId}")
