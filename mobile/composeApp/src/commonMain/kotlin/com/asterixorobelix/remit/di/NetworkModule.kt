package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.config.AppUpdateRepositoryImpl
import com.asterixorobelix.remit.data.network.NetworkConfig
import com.asterixorobelix.remit.data.network.createApiHttpClient
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.domain.update.AppUpdateRepository
import com.asterixorobelix.remit.domain.update.CheckForceUpdateUseCase
import com.asterixorobelix.remit.platform.AppConfig
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/** The API HttpClient. See [createApiHttpClient] for request IDs, base URL resolution, and retries. */
internal val networkModule = module {
    single { NetworkConfig.DEFAULT }
    single<HttpClient> {
        val preferences: AppPreferences = get()
        val appConfig: AppConfig = get()
        createApiHttpClient(
            config = get(),
            // Debug builds honour the Developer-screen override; release builds never do.
            apiBaseUrl = {
                preferences.getApiBaseUrlOverride()?.takeIf { appConfig.isDebug } ?: appConfig.apiBaseUrl
            },
        )
    }
}

/** Server-backed checks that exist only when the app ships with the backend capability. */
internal val backendModule = module {
    singleOf(::AppUpdateRepositoryImpl) bind AppUpdateRepository::class
    factoryOf(::CheckForceUpdateUseCase)
}
