package com.asterixorobelix.remit.platform

import android.util.Log

/**
 * Android implementation using Android's Log class
 */
class AndroidLogger : Logger {
    override var minLogLevel: LogLevel = LogLevel.DEBUG

    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.DEBUG) {
            if (throwable != null) {
                Log.d(tag, message, throwable)
            } else {
                Log.d(tag, message)
            }
        }
    }

    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.INFO) {
            if (throwable != null) {
                Log.i(tag, message, throwable)
            } else {
                Log.i(tag, message)
            }
        }
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.WARN) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.ERROR) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        when (level) {
            LogLevel.DEBUG -> d(tag, message, throwable)
            LogLevel.INFO -> i(tag, message, throwable)
            LogLevel.WARN -> w(tag, message, throwable)
            LogLevel.ERROR -> e(tag, message, throwable)
        }
    }
}

/**
 * Create Android-specific logger
 */
actual fun createLogger(): Logger = AndroidLogger()
