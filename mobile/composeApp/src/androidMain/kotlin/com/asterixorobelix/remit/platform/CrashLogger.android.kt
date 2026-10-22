package com.asterixorobelix.remit.platform

import android.util.Log

/**
 * Android crash reporting. With the `firebase` capability and a linked Crashlytics SDK, reports go
 * to Crashlytics via reflection (keep rules in `androidApp/proguard-rules.pro`); otherwise they
 * are written to logcat.
 */
class AndroidCrashLogger(private val firebaseEnabled: Boolean) : CrashLogger {
    private val tag = "CrashLogger"
    private var crashlytics: Any? = null

    override fun initialize() {
        if (!firebaseEnabled) {
            Log.i(tag, "Firebase capability disabled - logging errors locally")
            return
        }
        crashlytics = runCatching {
            Class.forName(CRASHLYTICS).getMethod("getInstance").invoke(null)
        }.onFailure {
            Log.w(tag, "Crashlytics unavailable (is google-services.json configured?)")
        }.getOrNull()
        invokeCrashlytics("setCrashlyticsCollectionEnabled", arrayOf(java.lang.Boolean.TYPE), true)
    }

    override fun logException(throwable: Throwable, message: String?) {
        message?.let { invokeCrashlytics("log", arrayOf(String::class.java), it) }
        if (!invokeCrashlytics("recordException", arrayOf(Throwable::class.java), throwable)) {
            Log.e(tag, message ?: "Exception occurred", throwable)
        }
    }

    override fun setCustomKey(key: String, value: String) {
        if (!invokeCrashlytics("setCustomKey", arrayOf(String::class.java, String::class.java), key, value)) {
            Log.d(tag, "Custom key: $key = $value")
        }
    }

    override fun setUserId(userId: String) {
        invokeCrashlytics("setUserId", arrayOf(String::class.java), userId)
    }

    override fun log(message: String) {
        if (!invokeCrashlytics("log", arrayOf(String::class.java), message)) Log.d(tag, message)
    }

    override fun testCrash() {
        error("Test crash for Firebase Crashlytics")
    }

    @Suppress("SpreadOperator") // Java reflection takes varargs.
    private fun invokeCrashlytics(method: String, types: Array<Class<*>>, vararg args: Any?): Boolean {
        val instance = crashlytics ?: return false
        return runCatching { instance.javaClass.getMethod(method, *types).invoke(instance, *args) }
            .onFailure { Log.e(tag, "Crashlytics call failed: $method", it) }
            .isSuccess
    }

    private companion object {
        const val CRASHLYTICS = "com.google.firebase.crashlytics.FirebaseCrashlytics"
    }
}

actual fun createCrashLogger(firebaseEnabled: Boolean): CrashLogger = AndroidCrashLogger(firebaseEnabled)
