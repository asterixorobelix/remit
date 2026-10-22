package com.example.myproject.domain.purchase

/**
 * Checks whether the current user can access a premium-gated feature.
 *
 * Use this to decide whether to show the paywall before performing an action.
 * Premium users always get [PremiumAccessResult.Allowed].
 * Free users get [PremiumAccessResult.PremiumRequired] with a trigger string
 * that is passed through to the paywall for context-specific copy and
 * post-purchase routing.
 *
 * Marked `open` so tests can subclass without a mocking framework.
 *
 * Usage:
 * ```kotlin
 * // In ViewModel:
 * val result = checkPremiumAccessUseCase(trigger = "export")
 * when (result) {
 *     is PremiumAccessResult.Allowed          -> performExport()
 *     is PremiumAccessResult.PremiumRequired  -> navigateToPaywall(result.trigger)
 * }
 * ```
 */
open class CheckPremiumAccessUseCase(
    private val premiumRepository: PremiumRepository,
) {
    /**
     * @param trigger A string identifying the blocked action (e.g. "export", "advanced_filter").
     *   Passed through to the paywall screen so it can show context-specific copy and
     *   route back to the right place after purchase.
     */
    open suspend operator fun invoke(trigger: String): PremiumAccessResult {
        return if (premiumRepository.isPremium.value) {
            PremiumAccessResult.Allowed
        } else {
            PremiumAccessResult.PremiumRequired(trigger)
        }
    }
}

/** Result of [CheckPremiumAccessUseCase]. */
sealed interface PremiumAccessResult {
    /** User has premium — proceed with the action. */
    data object Allowed : PremiumAccessResult

    /** User does not have premium — show the paywall with the given [trigger]. */
    data class PremiumRequired(val trigger: String) : PremiumAccessResult
}
