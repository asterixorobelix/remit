package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.config.AppUpdateRepositoryImpl
import com.asterixorobelix.remit.data.network.NetworkConfig
import com.asterixorobelix.remit.data.network.createApiHttpClient
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.domain.update.AppUpdateRepository
import com.asterixorobelix.remit.domain.update.CheckForceUpdateUseCase
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.shared.data.remote.ApiEndpoint
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** The API HttpClient and base URL. See [createApiHttpClient] for request IDs and retries. */
internal val networkModule = module {
    single { NetworkConfig.DEFAULT }
    single<HttpClient> { createApiHttpClient(config = get()) }
    single {
        val appConfig: AppConfig = get()
        val preferences: AppPreferences = get()
        ApiEndpoint { appConfig.resolveApiBaseUrl(preferences) }
    }
}

/** Server-backed checks that exist only when the app ships with the backend capability. */
internal val backendModule = module {
    singleOf(::AppUpdateRepositoryImpl) bind AppUpdateRepository::class
    factoryOf(::CheckForceUpdateUseCase)
}

/** Debug builds honour the Developer-screen override; release builds never read it. */
internal suspend fun AppConfig.resolveApiBaseUrl(preferences: AppPreferences): String =
    if (isDebug) preferences.getApiBaseUrlOverride() ?: apiBaseUrl else apiBaseUrl
