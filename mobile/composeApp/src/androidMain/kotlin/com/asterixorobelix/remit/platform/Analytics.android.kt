package com.asterixorobelix.remit.platform

import android.os.Bundle
import android.util.Log

/**
 * Android analytics. When the `firebase` capability is on and the Firebase SDK is linked (a real
 * `androidApp/google-services.json` is present), events go to Firebase Analytics through
 * reflection, because this shared module has no compile-time Firebase dependency. R8 keep rules
 * for the reflective calls live in `androidApp/proguard-rules.pro`. Otherwise events are logged.
 */
class AndroidAnalytics(private val firebaseEnabled: Boolean) : Analytics {
    private val tag = "Analytics"
    private var firebaseAnalytics: Any? = null
    private var isEnabled = true

    override fun initialize() {
        if (!firebaseEnabled) {
            Log.i(tag, "Firebase capability disabled - logging analytics locally")
            return
        }
        firebaseAnalytics = runCatching {
            val firebaseApp = Class.forName(FIREBASE_APP).getMethod("getInstance").invoke(null)
            val context = firebaseApp.javaClass.getMethod("getApplicationContext").invoke(firebaseApp)
            Class.forName(FIREBASE_ANALYTICS)
                .getMethod("getInstance", Class.forName("android.content.Context"))
                .invoke(null, context)
        }.onFailure {
            Log.w(tag, "Firebase Analytics unavailable (is google-services.json configured?)")
        }.getOrNull()
    }

    override fun logEvent(name: String, params: Map<String, Any>?) {
        if (!isEnabled) return
        val bundle = params?.toBundle() ?: Bundle()
        if (!invokeFirebase("logEvent", arrayOf(String::class.java, Bundle::class.java), name, bundle)) {
            val paramsString = params?.entries?.joinToString(", ") { "${it.key}=${it.value}" }.orEmpty()
            Log.d(tag, "Event: $name $paramsString")
        }
    }

    override fun logScreenView(screenName: String, screenClass: String?) {
        val params = mutableMapOf<String, Any>(AnalyticsParams.SCREEN_NAME to screenName)
        screenClass?.let { params[AnalyticsParams.SCREEN_CLASS] = it }
        logEvent(AnalyticsEvents.SCREEN_VIEW, params)
    }

    override fun setUserProperty(name: String, value: String?) {
        if (!isEnabled) return
        invokeFirebase("setUserProperty", arrayOf(String::class.java, String::class.java), name, value)
    }

    override fun setUserId(userId: String?) {
        if (!isEnabled) return
        invokeFirebase("setUserId", arrayOf(String::class.java), userId)
    }

    override fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        isEnabled = enabled
        invokeFirebase("setAnalyticsCollectionEnabled", arrayOf(java.lang.Boolean.TYPE), enabled)
    }

    override fun resetAnalyticsData() {
        invokeFirebase("resetAnalyticsData", emptyArray())
    }

    /** Calls a FirebaseAnalytics method; returns false when Firebase is not active. */
    @Suppress("SpreadOperator") // Java reflection takes varargs.
    private fun invokeFirebase(method: String, types: Array<Class<*>>, vararg args: Any?): Boolean {
        val instance = firebaseAnalytics ?: return false
        return runCatching { instance.javaClass.getMethod(method, *types).invoke(instance, *args) }
            .onFailure { Log.e(tag, "Firebase Analytics call failed: $method", it) }
            .isSuccess
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

    private companion object {
        const val FIREBASE_APP = "com.google.firebase.FirebaseApp"
        const val FIREBASE_ANALYTICS = "com.google.firebase.analytics.FirebaseAnalytics"
    }
}

actual fun createAnalytics(firebaseEnabled: Boolean): Analytics = AndroidAnalytics(firebaseEnabled)
