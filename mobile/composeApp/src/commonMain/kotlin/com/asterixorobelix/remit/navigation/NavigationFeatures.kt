package com.asterixorobelix.remit.navigation

import com.asterixorobelix.remit.platform.AppConfig

/**
 * Which optional destinations exist for this build. Navigation registers and links a route only
 * when it is available, so a disabled capability can never open a screen whose ViewModel is not
 * in the Koin graph. `NavigationGraphTest` resolves every available route's ViewModels.
 */
data class NavigationFeatures(
    val developerTools: Boolean,
) {
    companion object {
        fun from(config: AppConfig) = NavigationFeatures(
            developerTools = config.isDebug,
        )
    }
}

/** Whether this route can be reached with [features]. */
fun AppRoute.isAvailable(features: NavigationFeatures): Boolean = when (this) {
    AppRoute.Developer -> features.developerTools

    AppRoute.Onboarding,
    AppRoute.Home,
    AppRoute.Settings,
    AppRoute.Feedback,
    is AppRoute.WebView,
    // create-feature:available-routes
    -> true
}

/** Bottom-bar tabs in display order. */
fun NavigationFeatures.tabs(): List<AppRoute> =
    listOf(AppRoute.Home, AppRoute.Settings).filter { it.isAvailable(this) }
