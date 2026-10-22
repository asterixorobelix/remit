package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.Capability
import org.koin.core.module.Module

/**
 * Koin modules for a capability set. Core modules are always loaded; each optional feature is
 * added only when its capability is enabled, and navigation hides the matching routes otherwise
 * (see `navigation/NavigationFeatures.kt`). [platformModule] supplies `SqlDriverFactory`.
 */
fun appModules(config: AppConfig, platformModule: Module): List<Module> = buildList {
    val capabilities = config.capabilities
    add(observabilityModule(config))
    add(storageModule)
    add(networkModule)
    add(coreFeaturesModule)
    add(platformModule)
    if (Capability.BACKEND in capabilities) add(backendModule)
    // create-feature:modules
}
