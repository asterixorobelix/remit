package com.example.myproject.data.purchase

import android.content.Context
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.example.myproject.BuildConfig
import com.example.myproject.domain.purchase.PremiumRepository
import com.example.myproject.domain.purchase.PurchaseResult
import com.example.myproject.platform.ForegroundActivityTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Play Billing (8.x) implementation of [PremiumRepository] for a one-time premium product.
 *
 * - Registered only when the `billing` capability is enabled (see `MyProjectApplication`).
 * - Premium is granted only for `PURCHASED` purchases; `PENDING` purchases report
 *   [PurchaseResult.Pending] and are granted later, when Play delivers the completed purchase
 *   through the listener or the next `queryPurchasesAsync`.
 * - Purchases are acknowledged immediately to avoid Play's automatic refund after three days.
 * - The last known status is cached in SharedPreferences for cold starts.
 */
class AndroidPremiumRepository(
    context: Context,
    private val activityTracker: ForegroundActivityTracker,
) : PremiumRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutableIsPremium = MutableStateFlow(prefs.getBoolean(PREMIUM_STATUS_KEY, false))
    override val isPremium: StateFlow<Boolean> = mutableIsPremium.asStateFlow()

    private val pendingPurchase = MutableStateFlow<CompletableDeferred<PurchaseResult>?>(null)

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(PurchasesUpdatedListener(::handlePurchasesUpdated))
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) queryExistingPurchases()
            }

            override fun onBillingServiceDisconnected() = Unit // Auto-reconnect is enabled.
        })
    }

    @Suppress("ReturnCount")
    override suspend fun purchase(): PurchaseResult {
        val activity = activityTracker.currentActivity
            ?: return PurchaseResult.Error("No foreground activity to host the purchase sheet")
        if (pendingPurchase.value != null) return PurchaseResult.Error("A purchase is already in progress")
        val productDetails = queryProductDetails() ?: return PurchaseResult.Error("Product not found")

        val deferred = CompletableDeferred<PurchaseResult>()
        pendingPurchase.value = deferred
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).build()),
            )
            .build()
        val launchResult = billingClient.launchBillingFlow(activity, flowParams)
        if (launchResult.responseCode != BillingResponseCode.OK) {
            pendingPurchase.value = null
            return PurchaseResult.Error(launchResult.debugMessage)
        }
        return deferred.await()
    }

    override suspend fun restorePurchases(): PurchaseResult {
        val result = CompletableDeferred<PurchaseResult>()
        billingClient.queryPurchasesAsync(inAppPurchasesQuery()) { billingResult, purchases ->
            result.complete(
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    applyPurchases(purchases) ?: PurchaseResult.Error("No previous purchase found")
                } else {
                    PurchaseResult.Error(billingResult.debugMessage)
                },
            )
        }
        return result.await()
    }

    override suspend fun refreshStatus() = queryExistingPurchases()

    private fun handlePurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        val result = when (billingResult.responseCode) {
            BillingResponseCode.OK -> applyPurchases(purchases.orEmpty())
                ?: PurchaseResult.Error("Purchase did not include the premium product")

            BillingResponseCode.USER_CANCELED -> PurchaseResult.Cancelled

            BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryExistingPurchases()
                PurchaseResult.Success
            }

            else -> PurchaseResult.Error(billingResult.debugMessage)
        }
        pendingPurchase.value?.complete(result)
        pendingPurchase.value = null
    }

    /**
     * Grants premium for a completed purchase of our product and acknowledges it. Returns null when
     * [purchases] contains no purchase of the premium product.
     */
    private fun applyPurchases(purchases: List<Purchase>): PurchaseResult? {
        val premium = purchases.filter { PRODUCT_ID in it.products }
        val completed = premium.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        return when {
            completed != null -> {
                updatePremiumStatus(true)
                if (!completed.isAcknowledged) acknowledge(completed)
                PurchaseResult.Success
            }

            premium.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> PurchaseResult.Pending

            else -> null
        }
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(inAppPurchasesQuery()) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                if (applyPurchases(purchases) != PurchaseResult.Success) updatePremiumStatus(false)
            }
        }
    }

    private suspend fun queryProductDetails(): ProductDetails? {
        val deferred = CompletableDeferred<ProductDetails?>()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            deferred.complete(
                result.productDetailsList.firstOrNull()
                    ?.takeIf { billingResult.responseCode == BillingResponseCode.OK },
            )
        }
        return deferred.await()
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient.acknowledgePurchase(params) { }
    }

    private fun inAppPurchasesQuery() =
        QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()

    private fun updatePremiumStatus(isPremium: Boolean) {
        mutableIsPremium.value = isPremium
        prefs.edit { putBoolean(PREMIUM_STATUS_KEY, isPremium) }
    }

    private companion object {
        val PRODUCT_ID: String = BuildConfig.PLAY_BILLING_PRODUCT
        const val PREFS_NAME = "myproject_prefs"
        const val PREMIUM_STATUS_KEY = "myproject_premium_status"
    }
}
