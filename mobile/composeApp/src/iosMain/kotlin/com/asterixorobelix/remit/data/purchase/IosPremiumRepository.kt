package com.asterixorobelix.remit.data.purchase

import com.asterixorobelix.remit.domain.purchase.PremiumRepository
import com.asterixorobelix.remit.domain.purchase.PurchaseResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.resume

/**
 * iOS [PremiumRepository] on top of [StoreKitBridge]. The last known status is cached in
 * NSUserDefaults so cold starts render correctly before the entitlement check completes, and
 * `Transaction.updates` keeps it current (Ask to Buy approvals, refunds).
 */
class IosPremiumRepository(
    private val storeKitBridge: StoreKitBridge,
) : PremiumRepository {

    private val defaults = NSUserDefaults.standardUserDefaults
    private val mutableIsPremium = MutableStateFlow(defaults.boolForKey(PREMIUM_STATUS_KEY))
    override val isPremium: StateFlow<Boolean> = mutableIsPremium.asStateFlow()

    init {
        storeKitBridge.observeEntitlementChanges { entitled -> updateStatus(entitled.boolValue) }
        storeKitBridge.checkEntitlement { entitled -> updateStatus(entitled.boolValue) }
    }

    override suspend fun purchase(): PurchaseResult {
        val result = suspendCancellableCoroutine { continuation ->
            storeKitBridge.purchase { status, errorMessage ->
                val purchaseResult = when {
                    status.intValue == PurchaseStatus.SUCCESS -> PurchaseResult.Success
                    status.intValue == PurchaseStatus.PENDING -> PurchaseResult.Pending
                    errorMessage != null -> PurchaseResult.Error(errorMessage.toString())
                    else -> PurchaseResult.Cancelled
                }
                continuation.resume(purchaseResult)
            }
        }
        if (result is PurchaseResult.Success) refreshStatus()
        return result
    }

    override suspend fun restorePurchases(): PurchaseResult {
        val result = suspendCancellableCoroutine { continuation ->
            storeKitBridge.restorePurchases { success, errorMessage ->
                val purchaseResult = when {
                    success.boolValue -> PurchaseResult.Success
                    errorMessage != null -> PurchaseResult.Error(errorMessage.toString())
                    else -> PurchaseResult.Cancelled
                }
                continuation.resume(purchaseResult)
            }
        }
        if (result is PurchaseResult.Success) refreshStatus()
        return result
    }

    override suspend fun refreshStatus() {
        suspendCancellableCoroutine { continuation ->
            storeKitBridge.checkEntitlement { entitled ->
                updateStatus(entitled.boolValue)
                continuation.resume(Unit)
            }
        }
    }

    private fun updateStatus(isPremium: Boolean) {
        mutableIsPremium.value = isPremium
        defaults.setBool(isPremium, PREMIUM_STATUS_KEY)
    }

    private companion object {
        const val PREMIUM_STATUS_KEY = "remit_premium_status"
    }
}
