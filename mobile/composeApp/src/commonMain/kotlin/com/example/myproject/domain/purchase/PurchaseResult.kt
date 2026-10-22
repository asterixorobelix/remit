package com.example.myproject.domain.purchase

/**
 * Result of a purchase or restore operation.
 *
 * Usage:
 * ```kotlin
 * when (val result = premiumRepository.purchase()) {
 *     is PurchaseResult.Success   -> { /* grant access */ }
 *     is PurchaseResult.Cancelled -> { /* user cancelled, show nothing */ }
 *     is PurchaseResult.Pending   -> { /* tell the user the purchase is awaiting approval */ }
 *     is PurchaseResult.Error     -> { /* show a localized error; log result.message */ }
 * }
 * ```
 */
sealed interface PurchaseResult {
    data object Success : PurchaseResult
    data object Cancelled : PurchaseResult

    /**
     * The store accepted the order but payment is not complete yet (for example cash payments
     * or Ask to Buy). Do not grant access; the platform repository updates `isPremium` when the
     * purchase completes.
     */
    data object Pending : PurchaseResult

    /** [message] is diagnostic text from the store SDK; show a localized message instead. */
    data class Error(val message: String) : PurchaseResult
}
