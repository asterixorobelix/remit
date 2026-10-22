package com.example.myproject.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLog
import kotlin.experimental.ExperimentalNativeApi

/**
 * iOS implementation of AppConfig using Info.plist values.
 *
 * Values come from Info.plist, which expands build settings from
 * `iosApp/Configuration/Config.xcconfig` per build configuration (Debug/Release).
 * Keep that file in sync with `app.properties` via `scripts/sync-app-config.sh`.
 *
 * Optional Info.plist overrides: ANALYTICS_ENABLED, CRASH_REPORTING_ENABLED ("true"/"false").
 */
class IOSAppConfig : AppConfig {
    private val bundle = NSBundle.mainBundle
    override val capabilities: Set<Capability> by lazy {
        parseCapabilities(bundle.objectForInfoDictionaryKey("CAPABILITIES") as? String ?: "auth,offline,backend")
    }

    override val environment: Environment by lazy {
        val envString = bundle.objectForInfoDictionaryKey("APP_ENVIRONMENT") as? String
        Environment.fromString(envString).also {
            NSLog("[AppConfig] Environment: $it (from: $envString)")
        }
    }

    override val apiBaseUrl: String by lazy {
        (bundle.objectForInfoDictionaryKey("API_BASE_URL") as? String)
            ?: getDefaultApiUrl()
    }

    // Debug-only tooling must never appear in a Release (TestFlight/App Store) binary.
    @OptIn(ExperimentalNativeApi::class)
    override val isDebug: Boolean = Platform.isDebugBinary

    override val versionName: String by lazy {
        (bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String)
            ?: "1.0.0"
    }

    override val versionCode: Int by lazy {
        val buildNumber = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String
        buildNumber?.toIntOrNull() ?: 1
    }

    override val applicationId: String by lazy {
        bundle.bundleIdentifier ?: "com.example.myproject"
    }

    override val analyticsEnabled: Boolean by lazy {
        val enabled = bundle.objectForInfoDictionaryKey("ANALYTICS_ENABLED") as? String
        enabled?.lowercase() == "true" || environment == Environment.PRODUCTION
    }

    override val crashReportingEnabled: Boolean by lazy {
        val enabled = bundle.objectForInfoDictionaryKey("CRASH_REPORTING_ENABLED") as? String
        enabled?.lowercase() == "true" || environment != Environment.DEVELOPMENT
    }

    private fun getDefaultApiUrl(): String {
        return when (environment) {
            Environment.DEVELOPMENT -> "http://localhost:8085"
            Environment.STAGING -> "https://staging-api.example.com"
            Environment.PRODUCTION -> "https://api.example.com"
        }
    }
}

/**
 * Create iOS-specific AppConfig
 */
actual fun createAppConfig(): AppConfig = IOSAppConfig()
