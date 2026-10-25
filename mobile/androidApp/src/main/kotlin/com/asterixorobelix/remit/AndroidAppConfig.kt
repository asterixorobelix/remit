package com.asterixorobelix.remit

import com.asterixorobelix.remit.app.BuildConfig
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.platform.Environment
import com.asterixorobelix.remit.platform.parseCapabilities

/**
 * This build's configuration: app.properties and the build type, compiled into BuildConfig by
 * build.gradle.kts. The shared code receives it as an [AppConfig].
 */
internal class AndroidAppConfig : AppConfig {
    override val capabilities: Set<Capability> = parseCapabilities(BuildConfig.CAPABILITIES)
    override val environment: Environment = Environment.fromString(BuildConfig.ENVIRONMENT)
    override val apiBaseUrl: String = BuildConfig.API_BASE_URL
    override val isDebug: Boolean = BuildConfig.DEBUG
    override val versionName: String = BuildConfig.VERSION_NAME
    override val versionCode: Int = BuildConfig.VERSION_CODE

    /** The release package name (for the store listing), without the debug or staging suffix. */
    override val applicationId: String = BuildConfig.RELEASE_APPLICATION_ID
    override val analyticsEnabled: Boolean = BuildConfig.ANALYTICS_ENABLED
    override val crashReportingEnabled: Boolean = BuildConfig.CRASH_REPORTING_ENABLED
    override val firebaseEmulatorHost: String? =
        BuildConfig.FIREBASE_EMULATOR_HOST.takeIf { BuildConfig.DEBUG && it.isNotBlank() }
}
