package com.asterixorobelix.remit.platform

import androidx.compose.runtime.Composable

/**
 * Platform-specific in-app review trigger.
 *
 * When [shouldRequest] becomes true, launches the platform review dialog:
 * - Android: Google Play In-App Review API
 * - iOS: SKStoreReviewController
 *
 * [onReviewFlowFinish] is called after the review flow completes (or is dismissed)
 * to reset the trigger state.
 */
@Composable
expect fun RequestInAppReview(shouldRequest: Boolean, onReviewFlowFinish: () -> Unit)
