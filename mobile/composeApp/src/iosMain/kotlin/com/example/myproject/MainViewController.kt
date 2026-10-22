package com.example.myproject

import androidx.compose.ui.window.ComposeUIViewController
import com.example.myproject.data.db.IosSqlDriverFactory
import com.example.myproject.data.db.SqlDriverFactory
import com.example.myproject.data.purchase.IosPremiumRepository
import com.example.myproject.data.purchase.StoreKitBridge
import com.example.myproject.di.appModules
import com.example.myproject.domain.purchase.PremiumRepository
import com.example.myproject.platform.Capability
import com.example.myproject.platform.Observability
import com.example.myproject.platform.createAppConfig
import com.example.myproject.platform.has
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Called from `iOSApp.swift` before any UI is created.
 *
 * [storeKitBridge] is only used when the `billing` capability is enabled; apps without in-app
 * purchases can pass nil.
 */
fun startKoinIos(storeKitBridge: StoreKitBridge? = null) {
    val config = createAppConfig()
    val platformModule = module {
        single<SqlDriverFactory> { IosSqlDriverFactory() }
        if (storeKitBridge != null && config.has(Capability.BILLING)) {
            single<StoreKitBridge> { storeKitBridge }
            singleOf(::IosPremiumRepository) bind PremiumRepository::class
        }
    }
    val koin = startKoin { modules(appModules(config, platformModule)) }.koin
    koin.get<Observability>().initialize()
}

// Swift calls this factory by name (MainViewControllerKt.MainViewController()), so it keeps the
// type-style name that detekt and ktlint would otherwise reject.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
