package com.asterixorobelix.remit

import android.app.Application
import android.content.Context
import com.asterixorobelix.remit.data.db.AndroidSqlDriverFactory
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.preferences.androidPreferencesDataStore
import com.asterixorobelix.remit.di.appModules
import com.asterixorobelix.remit.di.firebaseSettings
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.Observability
import com.asterixorobelix.remit.platform.androidObservabilityProviders
import com.asterixorobelix.remit.shared.data.startFirebaseForEmulators
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Starts the app for [config] (the build's values, which only the app module knows): Firebase for
 * the local emulators when the build has no google-services.json, Koin, then crash reporting and
 * analytics. Call it once, from `Application.onCreate`.
 */
fun Application.startRemit(config: AppConfig) {
    startFirebaseForEmulators(this, config.firebaseSettings())
    val androidPlatformModule = module {
        single<Context> { this@startRemit }
        single<SqlDriverFactory> { AndroidSqlDriverFactory(get()) }
        single { androidPreferencesDataStore(this@startRemit) }
        single { androidObservabilityProviders(config) }
    }
    val koin = startKoin { modules(appModules(config, androidPlatformModule)) }.koin
    koin.get<Observability>().initialize()
}
