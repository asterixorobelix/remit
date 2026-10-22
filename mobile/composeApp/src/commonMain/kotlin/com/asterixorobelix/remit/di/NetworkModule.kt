package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.config.AppUpdateRepositoryImpl
import com.asterixorobelix.remit.data.network.NetworkConfig
import com.asterixorobelix.remit.data.network.createApiHttpClient
import com.asterixorobelix.remit.data.preferences.AppPreferences
import com.asterixorobelix.remit.data.session.SessionEvents
import com.asterixorobelix.remit.data.session.SessionRepositoryImpl
import com.asterixorobelix.remit.data.session.TokenStore
import com.asterixorobelix.remit.domain.session.SessionRepository
import com.asterixorobelix.remit.domain.update.AppUpdateRepository
import com.asterixorobelix.remit.domain.update.CheckForceUpdateUseCase
import com.asterixorobelix.remit.platform.AppConfig
import io.ktor.client.HttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * The API HttpClient plus session storage. See [createApiHttpClient] for request IDs, bearer
 * scoping, retries and refresh-token rotation.
 */
internal val networkModule = module {
    single { NetworkConfig.DEFAULT }
    singleOf(::TokenStore)
    singleOf(::SessionEvents)
    singleOf(::SessionRepositoryImpl) bind SessionRepository::class
    single<HttpClient> {
        val preferences: AppPreferences = get()
        val appConfig: AppConfig = get()
        createApiHttpClient(
            config = get(),
            // Debug builds honour the Developer-screen override; release builds never do.
            apiBaseUrl = {
                preferences.getApiBaseUrlOverride()?.takeIf { appConfig.isDebug } ?: appConfig.apiBaseUrl
            },
            tokenStore = get(),
            sessionEvents = get(),
        )
    }
}

/** Server-backed checks that exist only when the app ships with the backend capability. */
internal val backendModule = module {
    singleOf(::AppUpdateRepositoryImpl) bind AppUpdateRepository::class
    factoryOf(::CheckForceUpdateUseCase)
}
