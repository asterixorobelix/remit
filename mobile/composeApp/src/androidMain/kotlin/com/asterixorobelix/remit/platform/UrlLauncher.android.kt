package com.asterixorobelix.remit.platform

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import org.koin.core.context.GlobalContext

private val ALLOWED_SCHEMES = setOf("https", "http", "mailto", "market")

/** Opens [url] with an external app. Returns false if the scheme is not allowed or no app handles it. */
actual fun openUrl(url: String): Boolean {
    val uri = url.toUri()
    val scheme = uri.scheme?.lowercase()
    if (scheme !in ALLOWED_SCHEMES) return false
    val context = GlobalContext.getOrNull()?.getOrNull<Context>() ?: return false
    val action = if (scheme == "mailto") Intent.ACTION_SENDTO else Intent.ACTION_VIEW
    val intent = Intent(action, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    // ActivityNotFoundException means nothing can handle the URL (e.g. no mail app installed).
    return runCatching { context.startActivity(intent) }.isSuccess
}

/** Play Store app first, then the web listing. */
actual fun openStoreListing(listing: StoreListing): Boolean =
    openUrl("market://details?id=${listing.androidPackage}") ||
        openUrl("https://play.google.com/store/apps/details?id=${listing.androidPackage}")
