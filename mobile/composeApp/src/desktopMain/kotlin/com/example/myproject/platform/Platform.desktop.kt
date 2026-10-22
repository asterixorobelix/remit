package com.example.myproject.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.awt.Desktop
import java.net.URI

// The desktop gallery is a development tool that is never released, so its version is fixed.
private const val GALLERY_VERSION_CODE = 10000

private class DesktopAppConfig : AppConfig {
    override val capabilities = parseCapabilities(System.getProperty("app.capabilities", "offline"))
    override val environment = Environment.DEVELOPMENT
    override val apiBaseUrl = System.getProperty("app.apiUrl", "http://localhost:8085")
    override val isDebug = true
    override val versionName = "1.0.0"
    override val versionCode = GALLERY_VERSION_CODE
    override val applicationId = "com.example.myproject.desktop"
}
actual fun createAppConfig(): AppConfig = DesktopAppConfig()

private class ConsoleLogger : Logger {
    override var minLogLevel = LogLevel.DEBUG
    override fun d(tag: String, message: String, throwable: Throwable?) = log(LogLevel.DEBUG, tag, message, throwable)
    override fun i(tag: String, message: String, throwable: Throwable?) = log(LogLevel.INFO, tag, message, throwable)
    override fun w(tag: String, message: String, throwable: Throwable?) = log(LogLevel.WARN, tag, message, throwable)
    override fun e(tag: String, message: String, throwable: Throwable?) = log(LogLevel.ERROR, tag, message, throwable)
    override fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal < minLogLevel.ordinal) return
        val cause = throwable?.let { ": ${it.message}" }.orEmpty()
        println("[$level][$tag] $message$cause")
    }
}
actual fun createLogger(): Logger = ConsoleLogger()

private class ConsoleAnalytics : Analytics {
    override fun initialize() = Unit
    override fun logEvent(name: String, params: Map<String, Any>?) = println("[analytics] $name $params")
    override fun logScreenView(screenName: String, screenClass: String?) =
        logEvent("screen_view", mapOf("screen" to screenName))
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun setUserId(userId: String?) = Unit
    override fun setAnalyticsCollectionEnabled(enabled: Boolean) = Unit
    override fun resetAnalyticsData() = Unit
}
actual fun createAnalytics(firebaseEnabled: Boolean): Analytics = ConsoleAnalytics()

private class ConsoleCrashLogger : CrashLogger {
    override fun initialize() = Unit
    override fun logException(throwable: Throwable, message: String?) = throwable.printStackTrace()
    override fun setCustomKey(key: String, value: String) = Unit
    override fun setUserId(userId: String) = Unit
    override fun log(message: String) = println("[crash] $message")
    override fun testCrash(): Unit = error("Desktop test crash")
}
actual fun createCrashLogger(firebaseEnabled: Boolean): CrashLogger = ConsoleCrashLogger()
actual fun openUrl(url: String): Boolean {
    val uri = runCatching { URI(url) }.getOrNull()
    if (uri == null || !Desktop.isDesktopSupported()) return false
    val desktop = Desktop.getDesktop()
    return runCatching {
        when (uri.scheme?.lowercase()) {
            "http", "https" -> desktop.browse(uri)
            "mailto" -> desktop.mail(uri)
            else -> return false
        }
    }.isSuccess
}

actual fun openStoreListing(listing: StoreListing): Boolean =
    openUrl("https://play.google.com/store/apps/details?id=${listing.androidPackage}")

private object NoLocation : LocationService {
    override suspend fun checkPermission() = PermissionState.DENIED
    override suspend fun requestPermission() = PermissionState.DENIED
    override fun startLocationUpdates(): Flow<LocationData> = emptyFlow()
    override fun stopLocationUpdates() = Unit
    override suspend fun getCurrentLocation(): LocationData? = null
}
actual fun createLocationService(): LocationService = NoLocation

@Composable actual fun RequestInAppReview(shouldRequest: Boolean, onReviewFlowFinish: () -> Unit) {
    val currentOnReviewFlowFinish by rememberUpdatedState(onReviewFlowFinish)
    LaunchedEffect(shouldRequest) { if (shouldRequest) currentOnReviewFlowFinish() }
}

@Composable actual fun rememberLocationPermissionLauncher(onResult: (granted: Boolean) -> Unit): () -> Unit =
    { onResult(false) }
