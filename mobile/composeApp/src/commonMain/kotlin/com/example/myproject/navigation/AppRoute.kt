package com.example.myproject.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Login : AppRoute

    @Serializable
    data object Register : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Tasks : AppRoute

    @Serializable
    data object Notifications : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object Search : AppRoute

    @Serializable
    data class Detail(val itemId: String, val title: String, val completed: Boolean) : AppRoute

    @Serializable
    data object Profile : AppRoute

    @Serializable
    data object Feedback : AppRoute

    @Serializable
    data class WebView(val url: String, val title: String) : AppRoute

    @Serializable
    data object Map : AppRoute

    @Serializable
    data class Paywall(val trigger: String) : AppRoute

    @Serializable
    data object Developer : AppRoute
    // create-feature:routes
}
