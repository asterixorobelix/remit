package com.example.myproject.domain.purchase

/**
 * Orchestrates premium upgrade purchase and restore operations.
 *
 * Delegates to [PremiumRepository] for platform-specific payment handling.
 * Marked `open` so tests can subclass without a mocking framework.
 *
 * Usage:
 * ```kotlin
 * // In ViewModel:
 * val result = purchasePremiumUseCase()   // triggers a purchase
 * val restored = purchasePremiumUseCase.restore()   // restores purchases
 * ```
 */
open class PurchasePremiumUseCase(
    private val premiumRepository: PremiumRepository,
) {
    /** Initiate a purchase of the premium upgrade. */
    open suspend operator fun invoke(): PurchaseResult = premiumRepository.purchase()

    /** Restore previous purchases from the store. */
    open suspend fun restore(): PurchaseResult = premiumRepository.restorePurchases()
}
