package com.asterixorobelix.remit.platform

import android.util.Log

/** [Analytics] that writes events to logcat: builds with no analytics provider configured. */
class LogcatAnalytics : Analytics {
    private val tag = "Analytics"
    private var isEnabled = true

    override fun initialize() {
        Log.i(tag, "No analytics provider - logging events locally")
    }

    override fun logEvent(name: String, params: Map<String, Any>?) {
        if (!isEnabled) return
        val paramsString = params?.entries?.joinToString(", ") { "${it.key}=${it.value}" }.orEmpty()
        Log.d(tag, "Event: $name $paramsString")
    }

    override fun logScreenView(screenName: String, screenClass: String?) =
        logEvent(AnalyticsEvents.SCREEN_VIEW, mapOf(AnalyticsParams.SCREEN_NAME to screenName))

    override fun setUserProperty(name: String, value: String?) = Unit

    override fun setUserId(userId: String?) = Unit

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    override fun resetAnalyticsData() = Unit
}
