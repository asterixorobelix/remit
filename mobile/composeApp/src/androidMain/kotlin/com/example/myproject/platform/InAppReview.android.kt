package com.example.myproject.platform

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.android.play.core.review.ReviewManagerFactory

@Composable
actual fun RequestInAppReview(shouldRequest: Boolean, onReviewFlowFinish: () -> Unit) {
    val context = LocalContext.current
    val currentOnReviewFlowFinish by rememberUpdatedState(onReviewFlowFinish)

    LaunchedEffect(shouldRequest) {
        if (!shouldRequest) return@LaunchedEffect
        val activity = context as? Activity ?: run {
            currentOnReviewFlowFinish()
            return@LaunchedEffect
        }
        val manager = ReviewManagerFactory.create(context)
        manager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
                    .addOnCompleteListener { currentOnReviewFlowFinish() }
            } else {
                currentOnReviewFlowFinish()
            }
        }
    }
}
