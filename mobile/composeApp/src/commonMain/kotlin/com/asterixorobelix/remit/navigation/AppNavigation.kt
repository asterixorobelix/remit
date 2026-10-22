package com.asterixorobelix.remit.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.asterixorobelix.remit.domain.settings.StartDestination
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.screen_home_title
import com.asterixorobelix.remit.generated.resources.screen_settings_title
import com.asterixorobelix.remit.presentation.developer.DeveloperScreen
import com.asterixorobelix.remit.presentation.feedback.FeedbackScreen
import com.asterixorobelix.remit.presentation.home.HomeScreen
import com.asterixorobelix.remit.presentation.onboarding.OnboardingScreen
import com.asterixorobelix.remit.presentation.settings.SettingsNavigation
import com.asterixorobelix.remit.presentation.settings.SettingsScreen
import com.asterixorobelix.remit.presentation.webview.WebViewScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val TAB_FADE_MS = 200
private const val SLIDE_MS = 300

private data class TabSpec(
    val label: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private fun AppRoute.tabSpec(): TabSpec? = when (this) {
    AppRoute.Home -> TabSpec(Res.string.screen_home_title, Icons.Filled.Home, Icons.Outlined.Home)
    AppRoute.Settings -> TabSpec(Res.string.screen_settings_title, Icons.Filled.Settings, Icons.Outlined.Settings)
    else -> null
}

/**
 * Navigation graph. [startDestination] is resolved once by `AppViewModel`; [features] decides which
 * optional routes are registered.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: StartDestination,
    features: NavigationFeatures,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val tabs = remember(features) { features.tabs() }
    val startRoute: AppRoute = remember(startDestination) {
        when (startDestination) {
            StartDestination.ONBOARDING -> AppRoute.Onboarding
            StartDestination.HOME -> AppRoute.Home
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            val destination = navBackStackEntry?.destination
            if (destination != null && tabs.any { destination.isRoute(it) }) {
                BottomBar(tabs = tabs, destination = destination, navController = navController)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(SLIDE_MS))
            },
            exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(SLIDE_MS))
            },
            popExitTransition = { fadeOut(tween(TAB_FADE_MS)) },
        ) {
            entryGraph(navController)
            tabGraph(navController, features)
            secondaryGraph(navController, features, snackbarHostState)
        }
    }
}

@Composable
private fun BottomBar(tabs: List<AppRoute>, destination: NavDestination, navController: NavHostController) {
    NavigationBar {
        tabs.forEach { tab ->
            val spec = tab.tabSpec() ?: return@forEach
            val selected = destination.isRoute(tab)
            val label = stringResource(spec.label)
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateToTab(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                        contentDescription = label,
                    )
                },
                label = { Text(label) },
            )
        }
    }
}

private fun NavGraphBuilder.entryGraph(navController: NavHostController) {
    composable<AppRoute.Onboarding> {
        OnboardingScreen(onComplete = { navController.resetTo(AppRoute.Home) })
    }
}

private fun NavGraphBuilder.tabGraph(navController: NavHostController, features: NavigationFeatures) {
    composable<AppRoute.Home>(
        enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
        exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
    ) {
        HomeScreen()
    }
    composable<AppRoute.Settings>(
        enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
        exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
    ) {
        SettingsScreen(
            navigation = SettingsNavigation(
                toFeedback = { navController.navigate(AppRoute.Feedback) },
                toWebView = { url, title -> navController.navigate(AppRoute.WebView(url = url, title = title)) },
                toDeveloper = { navController.navigateIfAvailable(AppRoute.Developer, features) },
            ),
        )
    }
}

/** Screens pushed on top of a tab. Generated features are added here (`scripts/create-feature.sh`). */
private fun NavGraphBuilder.secondaryGraph(
    navController: NavHostController,
    features: NavigationFeatures,
    snackbarHostState: SnackbarHostState,
) {
    composable<AppRoute.Feedback> {
        FeedbackScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable<AppRoute.WebView> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.WebView>()
        WebViewScreen(url = route.url, title = route.title, onNavigateBack = { navController.popBackStack() })
    }
    if (features.developerTools) {
        composable<AppRoute.Developer> {
            DeveloperScreen(
                onMessage = { message -> snackbarHostState.showSnackbar(message) },
                onAppReset = { navController.resetTo(AppRoute.Onboarding) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
    // create-feature:destinations
}

private fun NavDestination.isRoute(route: AppRoute): Boolean = hasRoute(route::class)

/** Switches tabs with a per-tab back stack: Home is the root, other tabs save/restore their state. */
private fun NavHostController.navigateToTab(tab: AppRoute) {
    navigate(tab) {
        popUpTo<AppRoute.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Replaces the entire back stack with [route] (onboarding finished, local reset). */
private fun NavHostController.resetTo(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateIfAvailable(route: AppRoute, features: NavigationFeatures) {
    if (route.isAvailable(features)) navigate(route)
}
