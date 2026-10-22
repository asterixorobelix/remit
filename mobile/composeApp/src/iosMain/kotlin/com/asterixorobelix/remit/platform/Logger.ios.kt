package com.asterixorobelix.remit.platform

import platform.Foundation.NSLog

/**
 * iOS implementation using NSLog
 * For production apps, consider integrating with OSLog for better performance
 */
class IOSLogger : Logger {
    override var minLogLevel: LogLevel = LogLevel.DEBUG

    override fun d(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.DEBUG) {
            logMessage("DEBUG", tag, message, throwable)
        }
    }

    override fun i(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.INFO) {
            logMessage("INFO", tag, message, throwable)
        }
    }

    override fun w(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.WARN) {
            logMessage("WARN", tag, message, throwable)
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (minLogLevel <= LogLevel.ERROR) {
            logMessage("ERROR", tag, message, throwable)
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

    private fun logMessage(level: String, tag: String, message: String, throwable: Throwable?) {
        val logLine = buildString {
            append("[$level] $tag: $message")
            throwable?.let {
                append("\n${it::class.simpleName}: ${it.message}")
                val stackTrace = it.stackTraceToString()
                if (stackTrace.isNotEmpty()) {
                    append("\n$stackTrace")
                }
            }
        }
        NSLog(logLine)
    }
}

/**
 * Create iOS-specific logger
 */
actual fun createLogger(): Logger = IOSLogger()
