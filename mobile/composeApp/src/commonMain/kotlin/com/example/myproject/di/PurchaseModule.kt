package com.example.myproject.di

import com.example.myproject.domain.purchase.CheckPremiumAccessUseCase
import com.example.myproject.domain.purchase.PurchasePremiumUseCase
import com.example.myproject.presentation.paywall.PaywallViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * In-app purchase use cases and the paywall (billing capability). The platform
 * `PremiumRepository` is registered by the platform module only when billing is enabled:
 * `AndroidPremiumRepository` in `MyProjectApplication`, `IosPremiumRepository` in `startKoinIos`.
 */
val purchaseModule = module {
    factoryOf(::PurchasePremiumUseCase)
    factoryOf(::CheckPremiumAccessUseCase)
    viewModelOf(::PaywallViewModel)
}
