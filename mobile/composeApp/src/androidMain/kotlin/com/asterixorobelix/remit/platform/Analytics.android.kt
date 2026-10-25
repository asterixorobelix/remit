package com.asterixorobelix.remit.platform

import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Android analytics. When [firebaseEnabled] (the `firebase` capability, not the emulators) and
 * Firebase was started from `google-services.json`, events go to Firebase Analytics; otherwise
 * they are logged. Collection is off in the manifest until `setAnalyticsCollectionEnabled` turns
 * it on, so an emulator or unconfigured build never reports.
 */
class AndroidAnalytics(private val firebaseEnabled: Boolean) : Analytics {
    private val tag = "Analytics"
    private var firebaseAnalytics: FirebaseAnalytics? = null
    private var isEnabled = true

    override fun initialize() {
        if (!firebaseEnabled) {
            Log.i(tag, "Firebase analytics disabled - logging analytics locally")
            return
        }
        firebaseAnalytics = try {
            FirebaseAnalytics.getInstance(FirebaseApp.getInstance().applicationContext)
        } catch (notStarted: IllegalStateException) {
            Log.w(tag, "Firebase Analytics unavailable (is google-services.json configured?)", notStarted)
            null
        }
    }

    override fun logEvent(name: String, params: Map<String, Any>?) {
        if (!isEnabled) return
        val analytics = firebaseAnalytics
        if (analytics == null) {
            val paramsString = params?.entries?.joinToString(", ") { "${it.key}=${it.value}" }.orEmpty()
            Log.d(tag, "Event: $name $paramsString")
            return
        }
        analytics.logEvent(name, params?.toBundle() ?: Bundle())
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        val params = mutableMapOf<String, Any>(AnalyticsParams.SCREEN_NAME to screenName)
        screenClass?.let { params[AnalyticsParams.SCREEN_CLASS] = it }
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    override fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        firebaseAnalytics?.setUserProperty(name, value)
    }

    override fun setUserId(userId: String?) {
        if (!isEnabled) return
        firebaseAnalytics?.setUserId(userId)
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        isEnabled = enabled
        firebaseAnalytics?.setAnalyticsCollectionEnabled(enabled)
    }

    override fun resetAnalyticsData() {
        firebaseAnalytics?.resetAnalyticsData()
    }

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

actual fun createAnalytics(firebaseEnabled: Boolean): Analytics = AndroidAnalytics(firebaseEnabled)
