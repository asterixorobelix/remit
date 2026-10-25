package com.asterixorobelix.remit

import androidx.compose.ui.window.ComposeUIViewController
import com.asterixorobelix.remit.data.db.IosSqlDriverFactory
import com.asterixorobelix.remit.data.db.SqlDriverFactory
import com.asterixorobelix.remit.data.preferences.iosPreferencesDataStore
import com.asterixorobelix.remit.di.appModules
import com.asterixorobelix.remit.platform.IOSAppConfig
import com.asterixorobelix.remit.platform.Observability
import org.koin.core.context.startKoin
import org.koin.dsl.module

/** Called from `iOSApp.swift` before any UI is created. */
fun startKoinIos() {
    val config = IOSAppConfig()
    val platformModule = module {
        single<SqlDriverFactory> { IosSqlDriverFactory() }
        single { iosPreferencesDataStore() }
    }
    val koin = startKoin { modules(appModules(config, platformModule)) }.koin
    koin.get<Observability>().initialize()
}

// Swift calls this factory by name (MainViewControllerKt.MainViewController()), so it keeps the
// type-style name that detekt and ktlint would otherwise reject.
@Suppress("FunctionNaming", "ktlint:standard:function-naming")
fun MainViewController() = ComposeUIViewController { App() }
