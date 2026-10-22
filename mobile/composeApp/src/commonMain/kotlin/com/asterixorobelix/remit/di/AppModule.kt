package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.data.search.EmptySearchRepository
import com.asterixorobelix.remit.domain.search.SearchRepository
import com.asterixorobelix.remit.platform.AppConfig
import com.asterixorobelix.remit.platform.Capability
import com.asterixorobelix.remit.presentation.map.MapViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin modules for a capability set. Core modules are always loaded; each optional feature is
 * added only when its capability is enabled, and navigation hides the matching routes otherwise
 * (see `navigation/AppDestinations.kt`). [platformModule] supplies `SqlDriverFactory` and, when
 * billing is enabled, the platform `PremiumRepository`.
 */
fun appModules(config: AppConfig, platformModule: Module): List<Module> = buildList {
    val capabilities = config.capabilities
    add(observabilityModule(config))
    add(storageModule)
    add(networkModule)
    add(coreFeaturesModule)
    add(platformModule)
    if (Capability.BACKEND in capabilities) add(backendModule)
    if (Capability.AUTH in capabilities) add(authModule)
    if (Capability.OFFLINE in capabilities) {
        val syncEnabled = Capability.BACKEND in capabilities && Capability.AUTH in capabilities
        add(taskModule(syncEnabled))
    } else {
        add(module { singleOf(::EmptySearchRepository) bind SearchRepository::class })
    }
    if (Capability.MAPS in capabilities) {
        add(locationModule)
        add(module { viewModelOf(::MapViewModel) })
    }
    if (Capability.BILLING in capabilities) add(purchaseModule)
    // create-feature:modules
}
