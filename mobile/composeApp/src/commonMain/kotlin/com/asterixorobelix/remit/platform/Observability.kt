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

    fun nonFatal(error: Throwable, context: String) {
        val safeContext = context.replace(Regex("(?i)(token|password|email)=[^, ]+"), "$1=[redacted]")
        crashLogger.logException(error, safeContext)
    }
}
