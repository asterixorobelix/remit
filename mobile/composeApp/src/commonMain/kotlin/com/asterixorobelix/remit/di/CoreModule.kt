package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.db.AppDatabase
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.developer.PreferencesDeveloperSettingsRepository
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.data.preferences.AppSettingsRepositoryImpl
import com.asterixorobelix.remit.data.preferences.createAppPreferences
import com.asterixorobelix.remit.domain.developer.DeveloperSettingsRepository
import com.asterixorobelix.remit.domain.developer.GetApiBaseUrlOverrideUseCase
import com.asterixorobelix.remit.domain.developer.ResetDevelopmentStateUseCase
import com.asterixorobelix.remit.domain.developer.SetApiBaseUrlOverrideUseCase
import com.asterixorobelix.remit.domain.feedback.BuildFeedbackEmailUseCase
import com.asterixorobelix.remit.domain.feedback.ValidateFeedbackUseCase
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
import com.asterixorobelix.remit.presentation.onboarding.OnboardingViewModel
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
    // Opened lazily on first use; shared by generated features.
    single { AppDatabase(get<SqlDriverFactory>().create()) }
    singleOf(::AppSettingsRepositoryImpl) bind AppSettingsRepository::class
    singleOf(::PreferencesDeveloperSettingsRepository) bind DeveloperSettingsRepository::class
}

/** Screens and use cases available for every capability set. */
internal val coreFeaturesModule = module {
    factoryOf(::ObserveThemeModeUseCase)
    factoryOf(::SetThemeModeUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::RecordAppOpenUseCase)
    factoryOf(::MarkReviewRequestedUseCase)
    factoryOf(::ResolveStartDestinationUseCase)
    factoryOf(::ValidateFeedbackUseCase)
    factoryOf(::BuildFeedbackEmailUseCase)
    factoryOf(::GetApiBaseUrlOverrideUseCase)
    factoryOf(::SetApiBaseUrlOverrideUseCase)
    factoryOf(::ResetDevelopmentStateUseCase)

    factoryOf(::AppLaunchUseCases)
    // The force-update check exists only with the backend capability.
    viewModel { AppViewModel(get(), get(), get(), getOrNull()) }
    viewModelOf(::OnboardingViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::FeedbackViewModel)
    viewModelOf(::DeveloperViewModel)
}
