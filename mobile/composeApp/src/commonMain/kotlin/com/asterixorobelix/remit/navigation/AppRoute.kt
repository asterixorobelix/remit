package com.asterixorobelix.remit.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute {
    @Serializable
    data object Onboarding : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object History : AppRoute

    @Serializable
    data object Settings : AppRoute

    @Serializable
    data object Feedback : AppRoute

    @Serializable
    data class WebView(val url: String, val title: String) : AppRoute

    @Serializable
    data object Developer : AppRoute
    // create-feature:routes
}
