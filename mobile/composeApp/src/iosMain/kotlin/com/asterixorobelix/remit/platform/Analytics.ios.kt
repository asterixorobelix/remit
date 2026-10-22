package com.asterixorobelix.remit.platform

import platform.Foundation.NSLog

/**
 * iOS implementation using NSLog for analytics logging
 * For production apps, consider integrating Firebase Analytics iOS SDK
 * or alternative analytics solutions like Amplitude, Mixpanel, etc.
 */
class IOSAnalytics : Analytics {
    private var isEnabled = true
    private var userId: String? = null
    private val userProperties = mutableMapOf<String, String?>()

    override fun initialize() {
        NSLog("Analytics: iOS Analytics initialized")
    }

    override fun logEvent(name: String, params: Map<String, Any>?) {
        if (!isEnabled) return

        val paramsString = params?.entries?.joinToString(", ") { "${it.key}=${it.value}" } ?: ""
        NSLog("Analytics Event: $name${if (paramsString.isNotEmpty()) " [$paramsString]" else ""}")
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        val params = mutableMapOf<String, Any>(
            AnalyticsParams.SCREEN_NAME to screenName,
        )
        screenClass?.let { params[AnalyticsParams.SCREEN_CLASS] = it }
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    override fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return

        userProperties[name] = value
        NSLog("Analytics User Property: $name = $value")
    }

    override fun setUserId(userId: String?) {
        if (!isEnabled) return

        this.userId = userId
        NSLog("Analytics User ID: $userId")
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        isEnabled = enabled
        NSLog("Analytics Collection Enabled: $enabled")
    }

    override fun resetAnalyticsData() {
        userId = null
        userProperties.clear()
        NSLog("Analytics: Data reset")
    }
}

/**
 * Create iOS-specific analytics
 */
@Suppress("UnusedParameter") // Firebase iOS is not bundled; see FIREBASE_SETUP.md.
actual fun createAnalytics(firebaseEnabled: Boolean): Analytics = IOSAnalytics()
