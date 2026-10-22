package com.asterixorobelix.remit.presentation.paywall

import org.jetbrains.compose.resources.StringResource

/**
 * UI state for the paywall screen.
 *
 * Each state carries the [trigger] that caused the paywall to appear,
 * so callers can resume the interrupted action after purchase completes.
 *
 * Usage:
 * ```kotlin
 * // Navigate to paywall with a trigger that identifies the blocked action:
 * navController.navigate(PaywallRoute(trigger = "premium_feature"))
 *
 * // On purchase complete, use the trigger to route back to the right place:
 * onPurchaseComplete = { trigger ->
 *     when (trigger) {
 *         "export"  -> continueExport()
 *         "feature" -> continueFeature()
 *     }
 * }
 * ```
 */
sealed interface PaywallUiState {
    /** String that identifies which action triggered the paywall (e.g. "export", "feature"). */
    val trigger: String

    /** Paywall is visible and ready for user action. */
    data class Ready(override val trigger: String) : PaywallUiState

    /** Purchase flow is in progress. */
    data class Purchasing(override val trigger: String) : PaywallUiState

    /** Restore flow is in progress. */
    data class Restoring(override val trigger: String) : PaywallUiState

    /** The store is waiting for payment approval; access is granted when it completes. */
    data class Pending(override val trigger: String) : PaywallUiState

    /** Purchase or restore failed. */
    data class Error(override val trigger: String, val message: StringResource) : PaywallUiState
}

/** One-time effects emitted by [PaywallViewModel]. */
sealed interface PaywallEffect {
    /** Purchase completed successfully. Carries the original [trigger] for continuation routing. */
    data class PurchaseComplete(val trigger: String) : PaywallEffect
}
