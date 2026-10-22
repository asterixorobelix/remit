package com.asterixorobelix.remit.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import platform.StoreKit.SKStoreReviewController

@Composable
actual fun RequestInAppReview(shouldRequest: Boolean, onReviewFlowFinish: () -> Unit) {
    val currentOnReviewFlowFinish by rememberUpdatedState(onReviewFlowFinish)
    LaunchedEffect(shouldRequest) {
        if (!shouldRequest) return@LaunchedEffect
        SKStoreReviewController.requestReview()
        currentOnReviewFlowFinish()
    }
}
