package com.asterixorobelix.remit.platform

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * [Analytics] backed by Firebase Analytics: an adapter, the only class that calls it. Chosen by
 * `androidObservabilityProviders`. Collection is off in the manifest until
 * [setAnalyticsCollectionEnabled] turns it on.
 */
class FirebaseAnalyticsTracker(private val analytics: FirebaseAnalytics) : Analytics {
    private var isEnabled = true

    override fun initialize() = Unit

    override fun logEvent(name: String, params: Map<String, Any>?) {
        if (isEnabled) analytics.logEvent(name, params?.toBundle() ?: Bundle())
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        val params = mutableMapOf<String, Any>(AnalyticsParams.SCREEN_NAME to screenName)
        screenClass?.let { params[AnalyticsParams.SCREEN_CLASS] = it }
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    override fun setUserProperty(name: String, value: String?) {
        if (isEnabled) analytics.setUserProperty(name, value)
    }

    override fun setUserId(userId: String?) {
        if (isEnabled) analytics.setUserId(userId)
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        isEnabled = enabled
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    override fun resetAnalyticsData() = analytics.resetAnalyticsData()

    private fun Map<String, Any>.toBundle(): Bundle = Bundle().apply {
        for ((key, value) in this@toBundle) {
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Float -> putFloat(key, value)
                is Boolean -> putBoolean(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
}
