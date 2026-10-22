package com.asterixorobelix.remit.navigation

import androidx.lifecycle.ViewModel
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.di.appModules
import com.asterixorobelix.remit.domain.purchase.PremiumRepository
import com.asterixorobelix.remit.domain.purchase.PurchaseResult
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.presentation.app.AppViewModel
import com.asterixorobelix.remit.presentation.auth.LoginViewModel
import com.asterixorobelix.remit.presentation.auth.RegisterViewModel
import com.asterixorobelix.remit.presentation.developer.DeveloperViewModel
import com.asterixorobelix.remit.presentation.feedback.FeedbackViewModel
import com.asterixorobelix.remit.presentation.home.HomeViewModel
import com.asterixorobelix.remit.presentation.map.MapViewModel
import com.asterixorobelix.remit.presentation.notifications.NotificationsViewModel
import com.asterixorobelix.remit.presentation.onboarding.OnboardingViewModel
import com.asterixorobelix.remit.presentation.paywall.PaywallViewModel
import com.asterixorobelix.remit.presentation.profile.ProfileViewModel
import com.asterixorobelix.remit.presentation.search.SearchViewModel
import com.asterixorobelix.remit.presentation.settings.SettingsViewModel
import com.asterixorobelix.remit.presentation.tasks.TasksViewModel
import com.asterixorobelix.remit.testing.FakeAppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.Koin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Starts the real Koin graph for representative capability sets and resolves every ViewModel
 * that navigation can reach for that set, so a route can never outlive its Koin module.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NavigationGraphTest {

    private val capabilitySets: Map<String, Set<Capability>> = mapOf(
        "minimal" to emptySet(),
        "auth,offline,backend" to setOf(Capability.AUTH, Capability.OFFLINE, Capability.BACKEND),
        "offline only" to setOf(Capability.OFFLINE),
        "all" to Capability.entries.toSet(),
    )

    /** One instance of every route; the exhaustive `when` in [viewModelsFor] forces updates. */
    private val allRoutes: List<AppRoute> = listOf(
        AppRoute.Onboarding, AppRoute.Login, AppRoute.Register, AppRoute.Home, AppRoute.Tasks,
        AppRoute.Notifications, AppRoute.Settings, AppRoute.Search, AppRoute.Detail("id", "Title", false),
        AppRoute.Profile, AppRoute.Feedback, AppRoute.WebView("https://example.com", "Web"), AppRoute.Map,
        AppRoute.Paywall("test"), AppRoute.Developer,
        // create-feature:graph-routes
    )

    private fun viewModelsFor(route: AppRoute): List<KClass<out ViewModel>> = when (route) {
        AppRoute.Onboarding -> listOf(OnboardingViewModel::class)
        AppRoute.Login -> listOf(LoginViewModel::class)
        AppRoute.Register -> listOf(RegisterViewModel::class)
        AppRoute.Home -> listOf(HomeViewModel::class)
        AppRoute.Tasks -> listOf(TasksViewModel::class)
        AppRoute.Notifications -> listOf(NotificationsViewModel::class)
        AppRoute.Settings -> listOf(SettingsViewModel::class)
        AppRoute.Search -> listOf(SearchViewModel::class)
        is AppRoute.Detail -> emptyList()
        AppRoute.Profile -> listOf(ProfileViewModel::class)
        AppRoute.Feedback -> listOf(FeedbackViewModel::class)
        is AppRoute.WebView -> emptyList()
        AppRoute.Map -> listOf(MapViewModel::class)
        is AppRoute.Paywall -> listOf(PaywallViewModel::class)
        AppRoute.Developer -> listOf(DeveloperViewModel::class)
        // create-feature:graph-view-models
    }

    @BeforeTest
    fun setup() {
        // ViewModels launch work in init; a paused dispatcher keeps the graph check side-effect free.
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun everyReachableRouteResolvesItsViewModels() {
        for ((name, capabilities) in capabilitySets) {
            for (debug in listOf(true, false)) {
                assertReachableViewModelsResolve(name, capabilities, debug)
            }
        }
    }

    private fun assertReachableViewModelsResolve(name: String, capabilities: Set<Capability>, debug: Boolean) {
        val config = FakeAppConfig(capabilities = capabilities, isDebug = debug)
        val koin = startKoin { modules(appModules(config, testPlatformModule(capabilities))) }.koin
        try {
            val features = NavigationFeatures.from(config)
            assertNotNull(koin.get<AppViewModel>(), "$name: root ViewModel")
            allRoutes.filter { it.isAvailable(features) }.forEach { route ->
                viewModelsFor(route).forEach { viewModelClass ->
                    runCatching { koin.resolve(viewModelClass, route) }
                        .onFailure { fail("$name (debug=$debug): $route cannot resolve $viewModelClass: $it") }
                }
            }
        } finally {
            stopKoin()
        }
    }

    @Test
    fun disabledCapabilitiesHideRoutesAndTheirModules() {
        val config = FakeAppConfig(capabilities = emptySet(), isDebug = false)
        val koin = startKoin { modules(appModules(config, testPlatformModule(emptySet()))) }.koin
        val features = NavigationFeatures.from(config)

        listOf(AppRoute.Login, AppRoute.Register, AppRoute.Profile, AppRoute.Tasks, AppRoute.Map, AppRoute.Developer)
            .forEach { route -> assertEquals(false, route.isAvailable(features), "$route should be hidden") }
        assertEquals(false, AppRoute.Paywall("x").isAvailable(features))
        assertEquals(listOf(AppRoute.Home, AppRoute.Notifications, AppRoute.Settings), features.tabs())
        assertNull(koin.getOrNull<LoginViewModel>(), "auth module must not load without the auth capability")
    }

    private fun Koin.resolve(viewModelClass: KClass<out ViewModel>, route: AppRoute): Any =
        if (route is AppRoute.Paywall) {
            get(viewModelClass, null) { parametersOf(route.trigger) }
        } else {
            get(viewModelClass, null, null)
        }

    private fun testPlatformModule(capabilities: Set<Capability>): Module = module {
        single<SqlDriverFactory> {
            object : SqlDriverFactory {
                override fun create(): SqlDriver =
                    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
            }
        }
        if (Capability.BILLING in capabilities) {
            single<PremiumRepository> { FakePremiumRepository() }
        }
    }

    private class FakePremiumRepository : PremiumRepository {
        override val isPremium: StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun purchase(): PurchaseResult = PurchaseResult.Cancelled
        override suspend fun restorePurchases(): PurchaseResult = PurchaseResult.Cancelled
        override suspend fun refreshStatus() = Unit
    }
}
