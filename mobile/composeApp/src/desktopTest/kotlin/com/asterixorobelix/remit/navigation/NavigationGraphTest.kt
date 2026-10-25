package com.asterixorobelix.remit.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.preferences.DataStoreAppPreferences
import com.asterixorobelix.remit.data.preferences.createPreferencesDataStore
import com.asterixorobelix.remit.di.appModules
import com.asterixorobelix.remit.domain.update.CheckForceUpdateUseCase
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.presentation.app.AppViewModel
import com.asterixorobelix.remit.presentation.developer.DeveloperViewModel
import com.asterixorobelix.remit.presentation.feedback.FeedbackViewModel
import com.asterixorobelix.remit.presentation.onboarding.OnboardingViewModel
import com.asterixorobelix.remit.presentation.payments.history.PaymentHistoryViewModel
import com.asterixorobelix.remit.presentation.payments.send.SendPaymentViewModel
import com.asterixorobelix.remit.presentation.settings.SettingsViewModel
import com.asterixorobelix.remit.testing.FakeAppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import kotlin.io.path.createTempDirectory
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
        "backend" to setOf(Capability.BACKEND),
        "all" to Capability.entries.toSet(),
    )

    /** One instance of every route; the exhaustive `when` in [viewModelsFor] forces updates. */
    private val allRoutes: List<AppRoute> = listOf(
        AppRoute.Onboarding,
        AppRoute.Home,
        AppRoute.History,
        AppRoute.Settings,
        AppRoute.Feedback,
        AppRoute.WebView("https://example.com", "Web"),
        AppRoute.Developer,
        // create-feature:graph-routes
    )

    private fun viewModelsFor(route: AppRoute): List<KClass<out ViewModel>> = when (route) {
        AppRoute.Onboarding -> listOf(OnboardingViewModel::class)
        AppRoute.Home -> listOf(SendPaymentViewModel::class)
        AppRoute.History -> listOf(PaymentHistoryViewModel::class)
        AppRoute.Settings -> listOf(SettingsViewModel::class)
        AppRoute.Feedback -> listOf(FeedbackViewModel::class)
        is AppRoute.WebView -> emptyList()
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
        val koin = startKoin { modules(appModules(config, testPlatformModule())) }.koin
        try {
            val features = NavigationFeatures.from(config)
            assertNotNull(koin.get<AppViewModel>(), "$name: root ViewModel")
            allRoutes.filter { it.isAvailable(features) }.forEach { route ->
                viewModelsFor(route).forEach { viewModelClass ->
                    // koinViewModel() supplies the SavedStateHandle from the back stack entry.
                    runCatching { koin.get<Any>(viewModelClass, null) { parametersOf(SavedStateHandle()) } }
                        .onFailure { fail("$name (debug=$debug): $route cannot resolve $viewModelClass: $it") }
                }
            }
        } finally {
            stopKoin()
        }
    }

    @Test
    fun releaseBuildsWithoutCapabilitiesHideOptionalRoutesAndModules() {
        val config = FakeAppConfig(capabilities = emptySet(), isDebug = false)
        val koin = startKoin { modules(appModules(config, testPlatformModule())) }.koin
        val features = NavigationFeatures.from(config)

        assertEquals(false, AppRoute.Developer.isAvailable(features), "Developer tools are debug-only")
        assertEquals(listOf(AppRoute.Home, AppRoute.History, AppRoute.Settings), features.tabs())
        assertNull(koin.getOrNull<CheckForceUpdateUseCase>(), "backend module must not load without backend")
    }

    private fun testPlatformModule(): Module = module {
        single<SqlDriverFactory> {
            object : SqlDriverFactory {
                override fun create(): SqlDriver =
                    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also { AppDatabase.Schema.create(it) }
            }
        }
        // A fresh file per graph: DataStore allows one active store per file in a process.
        single {
            createPreferencesDataStore(
                createTempDirectory("remit-settings").resolve(DataStoreAppPreferences.FILE_NAME).toString(),
            )
        }
    }
}
