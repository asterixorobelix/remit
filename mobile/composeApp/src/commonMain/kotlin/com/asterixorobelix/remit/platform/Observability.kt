package com.asterixorobelix.remit.platform

class Observability(
    private val config: AppConfig,
    private val analytics: Analytics,
    private val crashLogger: CrashLogger,
) {
    fun initialize() {
        analytics.initialize()
        analytics.setAnalyticsCollectionEnabled(config.analyticsEnabled)
        crashLogger.initialize()
        crashLogger.setCustomKey("environment", config.environment.name)
        crashLogger.setCustomKey("version", config.versionName)
        analytics.logEvent("release_health_start", mapOf("version" to config.versionName))
    }

    fun screen(name: String) = analytics.logScreenView(name)

    /** The crash logger redacts personal data and credentials ([RedactingCrashLogger]). */
    fun nonFatal(error: Throwable, context: String) = crashLogger.logException(error, context)
}
