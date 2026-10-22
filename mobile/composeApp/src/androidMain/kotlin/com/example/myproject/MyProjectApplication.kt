package com.example.myproject

import android.app.Application
import android.content.Context
import com.example.myproject.data.db.AndroidSqlDriverFactory
import com.example.myproject.data.db.SqlDriverFactory
import com.example.myproject.data.preferences.AndroidAppPreferences
import com.example.myproject.data.purchase.AndroidPremiumRepository
import com.example.myproject.di.appModules
import com.example.myproject.domain.purchase.PremiumRepository
import com.example.myproject.platform.Capability
import com.example.myproject.platform.ForegroundActivityTracker
import com.example.myproject.platform.Observability
import com.example.myproject.platform.createAppConfig
import com.example.myproject.platform.has
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

class MyProjectApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidAppPreferences.init(this)
        val activityTracker = ForegroundActivityTracker().also(::registerActivityLifecycleCallbacks)
        val config = createAppConfig()
        val androidPlatformModule = module {
            single<Context> { this@MyProjectApplication }
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
