package com.asterixorobelix.remit

import android.app.Application
import android.content.Context
import com.asterixorobelix.remit.data.db.AndroidSqlDriverFactory
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.preferences.AndroidAppPreferences
import com.asterixorobelix.remit.data.purchase.AndroidPremiumRepository
import com.asterixorobelix.remit.di.appModules
import com.asterixorobelix.remit.domain.purchase.PremiumRepository
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.platform.ForegroundActivityTracker
import com.asterixorobelix.remit.platform.Observability
import com.asterixorobelix.remit.platform.createAppConfig
import com.asterixorobelix.remit.platform.has
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class RemitApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppPreferences.init(this)
        val activityTracker = ForegroundActivityTracker().also(::registerActivityLifecycleCallbacks)
        val config = createAppConfig()
        val androidPlatformModule = module {
            single<Context> { this@RemitApplication }
            single { activityTracker }
            single<SqlDriverFactory> { AndroidSqlDriverFactory(get()) }
            // Play Billing connects only for apps that ship the billing capability.
            if (config.has(Capability.BILLING)) {
                singleOf(::AndroidPremiumRepository) bind PremiumRepository::class
            }
        }
        val koin = startKoin { modules(appModules(config, androidPlatformModule)) }.koin
        koin.get<Observability>().initialize()
    }
}
