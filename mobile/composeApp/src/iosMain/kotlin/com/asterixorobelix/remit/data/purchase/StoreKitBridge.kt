package com.asterixorobelix.remit.data.purchase

import platform.Foundation.NSNumber
import platform.Foundation.NSString

/**
 * Kotlin-visible bridge for StoreKit 2 in-app purchases, implemented in Swift
 * (`iosApp/iosApp/StoreKitBridgeImpl.swift`) because StoreKit 2's async APIs are not callable
 * from Kotlin/Native. `iOSApp.swift` passes the instance to `startKoinIos`; the repository is
 * registered only when the `billing` capability is enabled.
 *
 * Callbacks use ObjC types (NSNumber/NSString) to avoid boxing issues across the bridge.
 */
interface StoreKitBridge {

    /** Receives NSNumber(true) if the user currently owns the premium product. */
    fun checkEntitlement(callback: (NSNumber) -> Unit)

    /**
     * Starts a purchase. The callback receives a [PurchaseStatus] code and, for failures, a
     * diagnostic message: `SUCCESS` (verified and finished), `PENDING` (Ask to Buy / SCA; not
     * granted yet), or `FAILED` (message == null means the user cancelled).
     */
    fun purchase(callback: (NSNumber, NSString?) -> Unit)

    /** Syncs with the App Store; success means an entitlement was found. */
    fun restorePurchases(callback: (NSNumber, NSString?) -> Unit)

    /**
     * Registers [callback] for entitlement changes delivered outside a purchase flow
     * (`Transaction.updates`: approved Ask to Buy, refunds, purchases on other devices).
     */
    fun observeEntitlementChanges(callback: (NSNumber) -> Unit)
}

/** Status codes passed by `StoreKitBridge.purchase`. Keep in sync with StoreKitBridgeImpl.swift. */
object PurchaseStatus {
    const val FAILED = 0
    const val SUCCESS = 1
    const val PENDING = 2
}
