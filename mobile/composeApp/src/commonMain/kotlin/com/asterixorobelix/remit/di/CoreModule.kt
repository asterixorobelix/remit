package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.developer.PreferencesDeveloperSettingsRepository
import com.asterixorobelix.remit.data.notifications.EmptyNotificationsRepository
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.data.preferences.AppSettingsRepositoryImpl
import com.asterixorobelix.remit.data.preferences.createAppPreferences
import com.asterixorobelix.remit.data.search.PreferencesRecentSearchesRepository
import com.asterixorobelix.remit.data.session.PreferencesProfileCache
import com.asterixorobelix.remit.domain.developer.DeveloperSettingsRepository
import com.asterixorobelix.remit.domain.developer.GetApiBaseUrlOverrideUseCase
import com.asterixorobelix.remit.domain.developer.ResetDevelopmentStateUseCase
import com.asterixorobelix.remit.domain.developer.SetApiBaseUrlOverrideUseCase
import com.asterixorobelix.remit.domain.feedback.BuildFeedbackEmailUseCase
import com.asterixorobelix.remit.domain.feedback.ValidateFeedbackUseCase
import com.asterixorobelix.remit.domain.home.GetDayPeriodUseCase
import com.asterixorobelix.remit.domain.notifications.MarkNotificationReadUseCase
import com.asterixorobelix.remit.domain.notifications.NotificationsRepository
import com.asterixorobelix.remit.domain.notifications.ObserveNotificationsUseCase
import com.asterixorobelix.remit.domain.notifications.RefreshNotificationsUseCase
import com.asterixorobelix.remit.domain.search.ClearRecentSearchesUseCase
import com.asterixorobelix.remit.domain.search.GetRecentSearchesUseCase
import com.asterixorobelix.remit.domain.search.RecentSearchesRepository
import com.asterixorobelix.remit.domain.search.SaveRecentSearchUseCase
import com.asterixorobelix.remit.domain.search.SearchUseCase
import com.asterixorobelix.remit.domain.session.ComputeInitialsUseCase
import com.asterixorobelix.remit.domain.session.GetCachedProfileUseCase
import com.asterixorobelix.remit.domain.session.ObserveSessionExpiredUseCase
import com.asterixorobelix.remit.domain.session.ProfileCache
import com.asterixorobelix.remit.domain.settings.AppSettingsRepository
import com.asterixorobelix.remit.domain.settings.CompleteOnboardingUseCase
import com.asterixorobelix.remit.domain.settings.MarkReviewRequestedUseCase
import com.asterixorobelix.remit.domain.settings.ObserveThemeModeUseCase
import com.asterixorobelix.remit.domain.settings.RecordAppOpenUseCase
import com.asterixorobelix.remit.domain.settings.ResolveStartDestinationUseCase
import com.asterixorobelix.remit.domain.settings.SetThemeModeUseCase
import com.asterixorobelix.remit.platform.Analytics
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.AppDispatchers
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.platform.CrashLogger
import com.asterixorobelix.remit.platform.DefaultAppDispatchers
import com.asterixorobelix.remit.platform.Logger
import com.asterixorobelix.remit.platform.Observability
import com.asterixorobelix.remit.platform.createAnalytics
import com.asterixorobelix.remit.platform.createCrashLogger
import com.asterixorobelix.remit.platform.createLogger
import com.asterixorobelix.remit.platform.has
import com.asterixorobelix.remit.presentation.app.AppLaunchUseCases
import com.asterixorobelix.remit.presentation.app.AppViewModel
import com.asterixorobelix.remit.presentation.developer.DeveloperViewModel
import com.asterixorobelix.remit.presentation.feedback.FeedbackViewModel
import com.asterixorobelix.remit.presentation.home.HomeViewModel
import com.asterixorobelix.remit.presentation.notifications.NotificationsViewModel
import com.asterixorobelix.remit.presentation.onboarding.OnboardingViewModel
import com.asterixorobelix.remit.presentation.search.SearchViewModel
import com.asterixorobelix.remit.presentation.settings.SettingsViewModel
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
