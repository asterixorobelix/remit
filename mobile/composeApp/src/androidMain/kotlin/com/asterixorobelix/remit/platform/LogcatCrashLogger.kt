package com.asterixorobelix.remit.platform

import android.util.Log

/** [CrashLogger] that writes to logcat: builds with no crash-reporting provider configured. */
class LogcatCrashLogger : CrashLogger {
    private val tag = "CrashLogger"

    override fun initialize() {
        Log.i(tag, "No crash-reporting provider - logging errors locally")
    }

    override fun logException(throwable: Throwable, message: String?) {
        Log.e(tag, message ?: "Exception occurred", throwable)
    }

    override fun setCustomKey(key: String, value: String) {
        Log.d(tag, "Custom key: $key = $value")
    }

    override fun setUserId(userId: String) = Unit

    override fun log(message: String) {
        Log.d(tag, message)
    }

    override fun testCrash() {
        error("Test crash")
    }
}
