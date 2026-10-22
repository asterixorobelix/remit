package com.example.myproject.platform

import com.example.myproject.BuildConfig

/**
 * Android implementation of AppConfig using BuildConfig fields.
 *
 * Configuration values are set at build time via build.gradle.kts
 * and can be customized via local.properties or environment variables.
 */
class AndroidAppConfig : AppConfig {
    override val capabilities: Set<Capability> = parseCapabilities(BuildConfig.CAPABILITIES)
    override val environment: Environment =
        Environment.fromString(BuildConfig.ENVIRONMENT)

    override val apiBaseUrl: String =
        BuildConfig.API_BASE_URL

    override val isDebug: Boolean =
        BuildConfig.DEBUG

    override val versionName: String =
        BuildConfig.VERSION_NAME

    override val versionCode: Int =
        BuildConfig.VERSION_CODE

    override val applicationId: String =
        BuildConfig.APPLICATION_ID

    override val analyticsEnabled: Boolean =
        BuildConfig.ANALYTICS_ENABLED

    override val crashReportingEnabled: Boolean =
        BuildConfig.CRASH_REPORTING_ENABLED
}

/**
 * Create Android-specific AppConfig
 */
actual fun createAppConfig(): AppConfig = AndroidAppConfig()
