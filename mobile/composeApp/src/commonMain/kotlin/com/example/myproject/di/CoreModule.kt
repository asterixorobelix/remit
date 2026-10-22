package com.example.myproject.di

import com.example.myproject.data.db.AppDatabase
import com.example.myproject.data.db.SqlDriverFactory
import com.example.myproject.data.developer.PreferencesDeveloperSettingsRepository
import com.example.myproject.data.notifications.EmptyNotificationsRepository
import com.example.myproject.data.preferences.AppPreferences
import com.example.myproject.data.preferences.AppSettingsRepositoryImpl
import com.example.myproject.data.preferences.createAppPreferences
import com.example.myproject.data.search.PreferencesRecentSearchesRepository
import com.example.myproject.data.session.PreferencesProfileCache
import com.example.myproject.domain.developer.DeveloperSettingsRepository
import com.example.myproject.domain.developer.GetApiBaseUrlOverrideUseCase
import com.example.myproject.domain.developer.ResetDevelopmentStateUseCase
import com.example.myproject.domain.developer.SetApiBaseUrlOverrideUseCase
import com.example.myproject.domain.feedback.BuildFeedbackEmailUseCase
import com.example.myproject.domain.feedback.ValidateFeedbackUseCase
import com.example.myproject.domain.home.GetDayPeriodUseCase
import com.example.myproject.domain.notifications.MarkNotificationReadUseCase
import com.example.myproject.domain.notifications.NotificationsRepository
import com.example.myproject.domain.notifications.ObserveNotificationsUseCase
import com.example.myproject.domain.notifications.RefreshNotificationsUseCase
import com.example.myproject.domain.search.ClearRecentSearchesUseCase
import com.example.myproject.domain.search.GetRecentSearchesUseCase
import com.example.myproject.domain.search.RecentSearchesRepository
import com.example.myproject.domain.search.SaveRecentSearchUseCase
import com.example.myproject.domain.search.SearchUseCase
import com.example.myproject.domain.session.ComputeInitialsUseCase
import com.example.myproject.domain.session.GetCachedProfileUseCase
import com.example.myproject.domain.session.ObserveSessionExpiredUseCase
import com.example.myproject.domain.session.ProfileCache
import com.example.myproject.domain.settings.AppSettingsRepository
import com.example.myproject.domain.settings.CompleteOnboardingUseCase
import com.example.myproject.domain.settings.MarkReviewRequestedUseCase
import com.example.myproject.domain.settings.ObserveThemeModeUseCase
import com.example.myproject.domain.settings.RecordAppOpenUseCase
import com.example.myproject.domain.settings.ResolveStartDestinationUseCase
import com.example.myproject.domain.settings.SetThemeModeUseCase
import com.example.myproject.platform.Analytics
import com.example.myproject.platform.AppConfig
import com.example.myproject.platform.AppDispatchers
import com.example.myproject.platform.Capability
import com.example.myproject.platform.CrashLogger
import com.example.myproject.platform.DefaultAppDispatchers
import com.example.myproject.platform.Logger
import com.example.myproject.platform.Observability
import com.example.myproject.platform.createAnalytics
import com.example.myproject.platform.createCrashLogger
import com.example.myproject.platform.createLogger
import com.example.myproject.platform.has
import com.example.myproject.presentation.app.AppLaunchUseCases
import com.example.myproject.presentation.app.AppViewModel
import com.example.myproject.presentation.developer.DeveloperViewModel
import com.example.myproject.presentation.feedback.FeedbackViewModel
import com.example.myproject.presentation.home.HomeViewModel
import com.example.myproject.presentation.notifications.NotificationsViewModel
import com.example.myproject.presentation.onboarding.OnboardingViewModel
import com.example.myproject.presentation.search.SearchViewModel
import com.example.myproject.presentation.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** Platform services: config, logging, crash reporting, analytics, dispatchers. */
internal fun observabilityModule(config: AppConfig): Module = module {
    single<AppConfig> { config }
    singleOf(::DefaultAppDispatchers) bind AppDispatchers::class
    single<CrashLogger> { createCrashLogger(firebaseEnabled = config.has(Capability.FIREBASE)) }
    single<Analytics> { createAnalytics(firebaseEnabled = config.has(Capability.FIREBASE)) }
    single<Logger> { createLogger() }
    singleOf(::Observability)
}

/** Device-local storage and the repositories built on it. Always loaded. */
internal val storageModule = module {
    single<AppPreferences> { createAppPreferences() }
    // Opened lazily on first use; shared by Tasks and generated features.
    single { AppDatabase(get<SqlDriverFactory>().create()) }
    singleOf(::AppSettingsRepositoryImpl) bind AppSettingsRepository::class
    singleOf(::PreferencesProfileCache) bind ProfileCache::class
    singleOf(::PreferencesRecentSearchesRepository) bind RecentSearchesRepository::class
    singleOf(::PreferencesDeveloperSettingsRepository) bind DeveloperSettingsRepository::class
    singleOf(::EmptyNotificationsRepository) bind NotificationsRepository::class
}

/** Screens and use cases available for every capability set. */
internal val coreFeaturesModule = module {
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::RecordAppOpenUseCase)
    factoryOf(::MarkReviewRequestedUseCase)
    factoryOf(::ResolveStartDestinationUseCase)
    factoryOf(::GetCachedProfileUseCase)
    factoryOf(::ObserveSessionExpiredUseCase)
    factoryOf(::ComputeInitialsUseCase)
    factoryOf(::GetDayPeriodUseCase)
    factoryOf(::SearchUseCase)
    factoryOf(::GetRecentSearchesUseCase)
    factoryOf(::SaveRecentSearchUseCase)
    factoryOf(::ClearRecentSearchesUseCase)
    factoryOf(::ObserveNotificationsUseCase)
    factoryOf(::RefreshNotificationsUseCase)
    factoryOf(::MarkNotificationReadUseCase)
    factoryOf(::ValidateFeedbackUseCase)
    factoryOf(::BuildFeedbackEmailUseCase)
    factoryOf(::GetApiBaseUrlOverrideUseCase)
    factoryOf(::SetApiBaseUrlOverrideUseCase)
    // Collects every feature's UserDataCleaner, which constructor injection cannot express.
    factory { ResetDevelopmentStateUseCase(get(), get(), getAll()) }

    // Optional collaborators (force-update check, recent tasks) exist only for some capabilities.
    factoryOf(::AppLaunchUseCases)
    viewModel { AppViewModel(get(), get(), get(), getOrNull()) }
    viewModel { HomeViewModel(get(), get(), get(), getOrNull()) }
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SearchViewModel)
    viewModelOf(::NotificationsViewModel)
    viewModelOf(::FeedbackViewModel)
    viewModelOf(::DeveloperViewModel)
}
