package com.asterixorobelix.remit.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.asterixorobelix.remit.generated.resources.screen_notifications_title
import com.asterixorobelix.remit.generated.resources.screen_settings_title
import com.asterixorobelix.remit.generated.resources.screen_tasks_title
import com.asterixorobelix.remit.presentation.auth.LoginScreen
import com.asterixorobelix.remit.presentation.auth.RegisterScreen
import com.asterixorobelix.remit.presentation.detail.DetailScreen
import com.asterixorobelix.remit.presentation.developer.DeveloperScreen
import com.asterixorobelix.remit.presentation.feedback.FeedbackScreen
import com.asterixorobelix.remit.presentation.home.HomeAction
import com.asterixorobelix.remit.presentation.home.HomeScreen
import com.asterixorobelix.remit.presentation.map.MapScreen
import com.asterixorobelix.remit.presentation.notifications.NotificationsScreen
import com.asterixorobelix.remit.presentation.onboarding.OnboardingScreen
import com.asterixorobelix.remit.presentation.paywall.PaywallScreen
import com.asterixorobelix.remit.presentation.paywall.PaywallViewModel
import com.asterixorobelix.remit.presentation.profile.ProfileScreen
import com.asterixorobelix.remit.presentation.search.SearchScreen
import com.asterixorobelix.remit.presentation.settings.SettingsNavigation
import com.asterixorobelix.remit.presentation.settings.SettingsScreen
import com.asterixorobelix.remit.presentation.tasks.TasksScreen
import com.asterixorobelix.remit.presentation.webview.WebViewScreen
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private const val TAB_FADE_MS = 200
private const val SLIDE_MS = 300
private const val HOME_PAYWALL_TRIGGER = "home"
private const val SETTINGS_PAYWALL_TRIGGER = "settings"

private data class TabSpec(
    val label: StringResource,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private fun AppRoute.tabSpec(): TabSpec? = when (this) {
    AppRoute.Home -> TabSpec(Res.string.screen_home_title, Icons.Filled.Home, Icons.Outlined.Home)

    AppRoute.Tasks -> TabSpec(Res.string.screen_tasks_title, Icons.Filled.Checklist, Icons.Outlined.Checklist)

    AppRoute.Notifications -> TabSpec(
        Res.string.screen_notifications_title,
        Icons.Filled.Notifications,
        Icons.Outlined.Notifications,
    )

    AppRoute.Settings -> TabSpec(Res.string.screen_settings_title, Icons.Filled.Settings, Icons.Outlined.Settings)

    else -> null
}

/**
 * Navigation graph. [startDestination] is resolved once by `AppViewModel`; [sessionExpiredSignal]
 * increments when the server revokes the session so the graph can return to sign-in.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    startDestination: StartDestination,
    features: NavigationFeatures,
    sessionExpiredSignal: Int,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val tabs = remember(features) { features.tabs() }
    val startRoute: AppRoute = remember(startDestination) {
        when (startDestination) {
            StartDestination.ONBOARDING -> AppRoute.Onboarding
            StartDestination.LOGIN -> AppRoute.Login
            StartDestination.HOME -> AppRoute.Home
        }
    }

    LaunchedEffect(sessionExpiredSignal) {
        if (sessionExpiredSignal > 0 && features.auth) navController.resetTo(AppRoute.Login)
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
            entryGraph(navController, features)
            tabGraph(navController, features, snackbarHostState)
            secondaryGraph(navController, features)
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

private fun NavGraphBuilder.entryGraph(navController: NavHostController, features: NavigationFeatures) {
    composable<AppRoute.Onboarding> {
        OnboardingScreen(
            onComplete = { navController.resetTo(if (features.auth) AppRoute.Login else AppRoute.Home) },
        )
    }
    if (features.auth) {
        composable<AppRoute.Login> {
            LoginScreen(
                onLoginSuccess = { navController.resetTo(AppRoute.Home) },
                onNavigateToRegister = { navController.navigate(AppRoute.Register) },
            )
        }
        composable<AppRoute.Register> {
            RegisterScreen(
                onRegisterSuccess = { navController.resetTo(AppRoute.Home) },
                onNavigateToLogin = { navController.popBackStack() },
            )
        }
    }
}

private fun NavGraphBuilder.tabGraph(
    navController: NavHostController,
    features: NavigationFeatures,
    snackbarHostState: SnackbarHostState,
) {
    composable<AppRoute.Home>(
        enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
        exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
    ) {
        HomeScreen(onAction = { action -> navController.openHomeAction(action, features) })
    }
    if (features.tasks) {
        composable<AppRoute.Tasks>(
            enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
            exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
        ) {
            TasksScreen(onMessage = { message -> snackbarHostState.showSnackbar(message) })
        }
    }
    composable<AppRoute.Notifications>(
        enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
        exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
    ) {
        NotificationsScreen()
    }
    composable<AppRoute.Settings>(
        enterTransition = { fadeIn(tween(TAB_FADE_MS)) },
        exitTransition = { fadeOut(tween(TAB_FADE_MS)) },
    ) {
        SettingsScreen(
            navigation = SettingsNavigation(
                toProfile = { navController.navigateIfAvailable(AppRoute.Profile, features) },
                toPremium = { navController.navigateIfAvailable(AppRoute.Paywall(SETTINGS_PAYWALL_TRIGGER), features) },
                toFeedback = { navController.navigate(AppRoute.Feedback) },
                toWebView = { url, title -> navController.navigate(AppRoute.WebView(url = url, title = title)) },
                toDeveloper = { navController.navigateIfAvailable(AppRoute.Developer, features) },
            ),
        )
    }
    // create-feature:destinations
}

private fun NavGraphBuilder.secondaryGraph(navController: NavHostController, features: NavigationFeatures) {
    composable<AppRoute.Search> {
        SearchScreen(
            onResultClick = { result ->
                navController.navigate(AppRoute.Detail(result.id, result.title, result.completed))
            },
            onNavigateBack = { navController.popBackStack() },
        )
    }
    composable<AppRoute.Detail> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.Detail>()
        DetailScreen(
            title = route.title,
            completed = route.completed,
            onNavigateBack = { navController.popBackStack() },
        )
    }
    composable<AppRoute.Feedback> {
        FeedbackScreen(onNavigateBack = { navController.popBackStack() })
    }
    composable<AppRoute.WebView> { backStackEntry ->
        val route = backStackEntry.toRoute<AppRoute.WebView>()
        WebViewScreen(url = route.url, title = route.title, onNavigateBack = { navController.popBackStack() })
    }
    if (features.auth) {
        composable<AppRoute.Profile> {
            ProfileScreen(
                onSignedOut = { navController.resetTo(AppRoute.Login) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
    if (features.maps) {
        composable<AppRoute.Map> {
            MapScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
    if (features.billing) {
        composable<AppRoute.Paywall> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Paywall>()
            val viewModel: PaywallViewModel = koinViewModel { parametersOf(route.trigger) }
            PaywallScreen(
                viewModel = viewModel,
                onDismiss = { navController.popBackStack() },
                onPurchaseComplete = { navController.popBackStack() },
            )
        }
    }
    if (features.developerTools) {
        composable<AppRoute.Developer> {
            DeveloperScreen(
                onAppReset = { navController.resetTo(AppRoute.Onboarding) },
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
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

/** Replaces the entire back stack with [route] (sign-in, sign-out, onboarding finished, reset). */
private fun NavHostController.resetTo(route: AppRoute) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

private fun NavHostController.navigateIfAvailable(route: AppRoute, features: NavigationFeatures) {
    if (route.isAvailable(features)) navigate(route)
}

private fun NavHostController.openHomeAction(action: HomeAction, features: NavigationFeatures) {
    when (action) {
        HomeAction.TASKS -> if (AppRoute.Tasks.isAvailable(features)) navigateToTab(AppRoute.Tasks)
        HomeAction.SETTINGS -> navigateToTab(AppRoute.Settings)
        HomeAction.SEARCH -> navigate(AppRoute.Search)
        HomeAction.PROFILE -> navigateIfAvailable(AppRoute.Profile, features)
        HomeAction.MAP -> navigateIfAvailable(AppRoute.Map, features)
        HomeAction.PREMIUM -> navigateIfAvailable(AppRoute.Paywall(HOME_PAYWALL_TRIGGER), features)
    }
}
