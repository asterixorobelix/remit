package com.example.myproject.platform

/**
 * Cross-platform analytics interface
 * Provides consistent analytics tracking for both Android and iOS
 *
 * Usage:
 * ```kotlin
 * val analytics: Analytics = koinInject()
 * analytics.logEvent("button_click", mapOf("button_name" to "submit"))
 * analytics.setUserProperty("subscription_type", "premium")
 * ```
 */
interface Analytics {
    /**
     * Initialize analytics
     * Should be called early in app lifecycle
     */
    fun initialize()

    /**
     * Log a custom event
     * @param name The event name (should be snake_case, max 40 chars)
     * @param params Optional parameters for the event
     */
    fun logEvent(name: String, params: Map<String, Any>? = null)

    /**
     * Log a screen view event
     * @param screenName The name of the screen
     * @param screenClass Optional class name of the screen
     */
    fun logScreenView(screenName: String, screenClass: String? = null)

    /**
     * Set a user property
     * @param name The property name
     * @param value The property value (null to clear)
     */
    fun setUserProperty(name: String, value: String?)

    /**
     * Set the user ID for analytics
     * @param userId The user identifier (null to clear)
     */
    fun setUserId(userId: String?)

    /**
     * Enable or disable analytics collection
     * @param enabled Whether to enable analytics
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean)

    /**
     * Reset analytics data (useful for logout)
     */
    fun resetAnalyticsData()
}

/**
 * Common analytics event names
 * Use these constants for consistency across the app
 */
object AnalyticsEvents {
    // Screen events
    const val SCREEN_VIEW = "screen_view"

    // User actions
    const val BUTTON_CLICK = "button_click"
    const val ITEM_SELECT = "item_select"
    const val SEARCH = "search"
    const val SHARE = "share"

    // Content events
    const val CONTENT_VIEW = "content_view"
    const val LIST_VIEW = "list_view"

    // User journey
    const val SIGN_UP = "sign_up"
    const val LOGIN = "login"
    const val LOGOUT = "logout"

    // Errors
    const val ERROR = "error"
    const val ERROR_SHOWN = "error_shown"

    // App lifecycle
    const val APP_OPEN = "app_open"
    const val APP_UPDATE = "app_update"
    const val FIRST_OPEN = "first_open"
}

/**
 * Common analytics parameter names
 */
object AnalyticsParams {
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
    const val BUTTON_NAME = "button_name"
    const val ITEM_ID = "item_id"
    const val ITEM_NAME = "item_name"
    const val ITEM_CATEGORY = "item_category"
    const val SEARCH_TERM = "search_term"
    const val CONTENT_TYPE = "content_type"
    const val ERROR_MESSAGE = "error_message"
    const val ERROR_CODE = "error_code"
    const val SUCCESS = "success"
    const val METHOD = "method"
    const val VALUE = "value"
}

/**
 * Platform analytics. With [firebaseEnabled] (the `firebase` capability) Android forwards events
 * to Firebase Analytics when the SDK is linked; otherwise events are only logged locally.
 */
expect fun createAnalytics(firebaseEnabled: Boolean): Analytics
