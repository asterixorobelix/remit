package com.asterixorobelix.remit.domain.purchase

import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for premium/paid subscription status.
 *
 * Provides reactive observation of premium status and purchase/restore operations.
 * Platform-specific implementations use StoreKit 2 (iOS) or Play Billing (Android).
 *
 * Setup:
 * 1. Set PLAY_BILLING_PRODUCT / IOS_BILLING_PRODUCT in `app.properties`.
 * 2. Enable the `billing` capability. Platform bindings are registered only then
 *    (`RemitApplication` on Android, `startKoinIos()` on iOS with `StoreKitBridgeImpl`).
 *
 * Usage:
 * ```kotlin
 * val isPremium by premiumRepository.isPremium.collectAsState()
 * ```
 */
interface PremiumRepository {
    /** Current premium status. True if user has an active premium purchase. */
    val isPremium: StateFlow<Boolean>

    /** Initiate a purchase of the premium upgrade. */
    suspend fun purchase(): PurchaseResult

    /** Restore previous purchases from the store. */
    suspend fun restorePurchases(): PurchaseResult

    /** Refresh premium status from the platform store. */
    suspend fun refreshStatus()
}
