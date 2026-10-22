package com.example.myproject.navigation

import com.example.myproject.platform.AppConfig
import com.example.myproject.platform.Capability
import com.example.myproject.platform.has

/**
 * Which optional destinations exist for this build. Navigation registers and links a route only
 * when it is available, so a disabled capability can never open a screen whose ViewModel is not
 * in the Koin graph. `NavigationGraphTest` resolves every available route's ViewModels.
 */
data class NavigationFeatures(
    val auth: Boolean,
    val tasks: Boolean,
    val maps: Boolean,
    val billing: Boolean,
    val developerTools: Boolean,
) {
    companion object {
        fun from(config: AppConfig) = NavigationFeatures(
            auth = config.has(Capability.AUTH),
            tasks = config.has(Capability.OFFLINE),
            maps = config.has(Capability.MAPS),
            billing = config.has(Capability.BILLING),
            developerTools = config.isDebug,
        )
    }
}

/** Whether this route can be reached with [features]. */
fun AppRoute.isAvailable(features: NavigationFeatures): Boolean = when (this) {
    AppRoute.Login, AppRoute.Register, AppRoute.Profile -> features.auth

    AppRoute.Tasks -> features.tasks

    AppRoute.Map -> features.maps

    is AppRoute.Paywall -> features.billing

    AppRoute.Developer -> features.developerTools

    AppRoute.Onboarding,
    AppRoute.Home,
    AppRoute.Notifications,
    AppRoute.Settings,
    AppRoute.Search,
    is AppRoute.Detail,
    AppRoute.Feedback,
    is AppRoute.WebView,
    // create-feature:available-routes
    -> true
}

/** Bottom-bar tabs in display order. */
fun NavigationFeatures.tabs(): List<AppRoute> =
    listOf(AppRoute.Home, AppRoute.Tasks, AppRoute.Notifications, AppRoute.Settings).filter { it.isAvailable(this) }
