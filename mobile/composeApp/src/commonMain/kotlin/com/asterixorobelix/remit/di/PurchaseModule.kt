package com.asterixorobelix.remit.di

import com.asterixorobelix.remit.domain.purchase.CheckPremiumAccessUseCase
import com.asterixorobelix.remit.domain.purchase.PurchasePremiumUseCase
import com.asterixorobelix.remit.presentation.paywall.PaywallViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * In-app purchase use cases and the paywall (billing capability). The platform
 * `PremiumRepository` is registered by the platform module only when billing is enabled:
 * `AndroidPremiumRepository` in `RemitApplication`, `IosPremiumRepository` in `startKoinIos`.
 */
val purchaseModule = module {
    factoryOf(::PurchasePremiumUseCase)
    factoryOf(::CheckPremiumAccessUseCase)
    viewModelOf(::PaywallViewModel)
}
