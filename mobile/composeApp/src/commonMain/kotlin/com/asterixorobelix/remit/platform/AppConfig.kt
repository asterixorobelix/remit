package com.asterixorobelix.remit.platform

/**
 * Application environment configuration.
 *
 * Provides environment-specific settings for API URLs, feature flags,
 * and other configuration values. Supports dev/staging/production environments.
 *
 * Usage:
 * ```kotlin
 * val config: AppConfig = koinInject()
 * val apiUrl = config.apiBaseUrl
 * if (config.isDebug) { /* debug-only code */ }
 * ```
 */
interface AppConfig {
    val capabilities: Set<Capability>

    /** Current environment (development, staging, production) */
    val environment: Environment

    /** Base URL for API requests */
    val apiBaseUrl: String

    /** Whether the app is running in debug mode */
    val isDebug: Boolean

    /** Application version name (e.g., "1.0.0") */
    val versionName: String

    /** Application version code (e.g., 10000) */
    val versionCode: Int

    /** Application package/bundle identifier */
    val applicationId: String

    /**
     * Debug builds only: the host of a local Firebase Emulator Suite (for example `10.0.2.2` from
     * the Android emulator). Null means the real Firebase project.
     */
    val firebaseEmulatorHost: String?
        get() = null

    /** Whether analytics collection is enabled */
    val analyticsEnabled: Boolean
        get() = environment != Environment.DEVELOPMENT

    /** Whether crash reporting is enabled */
    val crashReportingEnabled: Boolean
        get() = environment != Environment.DEVELOPMENT

    /** Whether verbose logging is enabled */
    val verboseLoggingEnabled: Boolean
        get() = environment == Environment.DEVELOPMENT
}

enum class Capability { BACKEND, FIREBASE }

fun AppConfig.has(capability: Capability): Boolean = capability in capabilities

internal fun parseCapabilities(value: String?): Set<Capability> = value.orEmpty()
    .split(',')
    .mapNotNull { raw -> Capability.entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } }
    .toSet()

/**
 * Application environment types.
 */
enum class Environment {
    /** Local development environment */
    DEVELOPMENT,

    /** Staging/QA environment */
    STAGING,

    /** Production environment */
    PRODUCTION,

    ;

    companion object {
        /**
         * Parse environment from string value.
         * Defaults to DEVELOPMENT if unrecognized.
         */
        fun fromString(value: String?): Environment {
            return when (value?.uppercase()) {
                "PRODUCTION", "PROD", "RELEASE" -> PRODUCTION
                "STAGING", "STAGE", "QA" -> STAGING
                else -> DEVELOPMENT
            }
        }
    }
}

/**
 * Factory function to create platform-specific AppConfig.
 * Implemented in androidMain and iosMain.
 */
expect fun createAppConfig(): AppConfig
