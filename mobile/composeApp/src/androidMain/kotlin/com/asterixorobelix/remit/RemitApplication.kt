package com.asterixorobelix.remit

import android.app.Application
import android.content.Context
import com.asterixorobelix.remit.data.db.AndroidSqlDriverFactory
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.preferences.AndroidAppPreferences
import com.asterixorobelix.remit.di.appModules
import com.asterixorobelix.remit.platform.Observability
import com.asterixorobelix.remit.platform.createAppConfig
import org.koin.core.context.startKoin
import org.koin.dsl.module

class RemitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppPreferences.init(this)
        val config = createAppConfig()
        val androidPlatformModule = module {
            single<Context> { this@RemitApplication }
            single<SqlDriverFactory> { AndroidSqlDriverFactory(get()) }
        }
        val koin = startKoin { modules(appModules(config, androidPlatformModule)) }.koin
        koin.get<Observability>().initialize()
    }
}
