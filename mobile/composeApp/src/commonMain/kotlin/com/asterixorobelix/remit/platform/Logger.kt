package com.asterixorobelix.remit.platform

/**
 * Log level enum for filtering log output
 */
enum class LogLevel {
    DEBUG,
    INFO,
    WARN,
    ERROR,
}

/**
 * Cross-platform logging interface
 * Provides consistent logging capabilities for both Android and iOS
 *
 * Usage:
 * ```kotlin
 * val logger: Logger = koinInject()
 * logger.d("MyScreen", "User clicked button")
 * logger.e("MyScreen", "Failed to load data", exception)
 * ```
 */
interface Logger {
    /**
     * Current minimum log level
     * Messages below this level will be ignored
     */
    var minLogLevel: LogLevel

    /**
     * Log a debug message
     * @param tag The tag/category for the log message
     * @param message The message to log
     * @param throwable Optional exception to include
     */
    fun d(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log an info message
     * @param tag The tag/category for the log message
     * @param message The message to log
     * @param throwable Optional exception to include
     */
    fun i(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log a warning message
     * @param tag The tag/category for the log message
     * @param message The message to log
     * @param throwable Optional exception to include
     */
    fun w(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log an error message
     * @param tag The tag/category for the log message
     * @param message The message to log
     * @param throwable Optional exception to include
     */
    fun e(tag: String, message: String, throwable: Throwable? = null)

    /**
     * Log a message with specified level
     * @param level The log level
     * @param tag The tag/category for the log message
     * @param message The message to log
     * @param throwable Optional exception to include
     */
    fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
}

/**
 * Extension function to create a tagged logger
 * Useful for creating a logger with a fixed tag for a class
 *
 * Usage:
 * ```kotlin
 * class MyViewModel(logger: Logger) {
 *     private val log = logger.withTag("MyViewModel")
 *
 *     fun doSomething() {
 *         log.d("Doing something")
 *     }
 * }
 * ```
 */
fun Logger.withTag(tag: String): TaggedLogger = TaggedLogger(this, tag)

/**
 * Logger wrapper with a fixed tag
 */
class TaggedLogger(
    private val logger: Logger,
    private val tag: String,
) {
    fun d(message: String, throwable: Throwable? = null) = logger.d(tag, message, throwable)
    fun i(message: String, throwable: Throwable? = null) = logger.i(tag, message, throwable)
    fun w(message: String, throwable: Throwable? = null) = logger.w(tag, message, throwable)
    fun e(message: String, throwable: Throwable? = null) = logger.e(tag, message, throwable)
}

/**
 * Expected platform-specific implementation
 */
expect fun createLogger(): Logger
