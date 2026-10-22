package com.example.myproject.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens

private const val SHIMMER_ANIMATION_DURATION_MS = 1200
private const val SHIMMER_START_OFFSET = 0f
private const val SHIMMER_END_OFFSET = 1000f
private const val SHIMMER_GRADIENT_START = 0.0f
private const val SHIMMER_GRADIENT_CENTER = 0.5f
private const val SHIMMER_GRADIENT_END = 1.0f

/**
 * Shimmer loading placeholder that matches content shape.
 *
 * Uses an animated gradient that sweeps from left to right to indicate loading.
 * Pair with a skeleton layout that mirrors your real content for best results.
 *
 * Usage:
 * ```kotlin
 * // Single line placeholder
 * ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(16.dp).clip(MaterialTheme.shapes.small))
 *
 * // Card skeleton matching real layout
 * @Composable fun CardSkeleton() {
 *     Column(Modifier.padding(16.dp)) {
 *         ShimmerBox(Modifier.fillMaxWidth().height(120.dp))  // image
 *         Spacer(Modifier.height(8.dp))
 *         ShimmerBox(Modifier.fillMaxWidth(0.6f).height(16.dp).clip(MaterialTheme.shapes.small))  // title
 *         Spacer(Modifier.height(4.dp))
 *         ShimmerBox(Modifier.fillMaxWidth(0.4f).height(14.dp).clip(MaterialTheme.shapes.small))  // subtitle
 *     }
 * }
 * ```
 */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = SHIMMER_START_OFFSET,
        targetValue = SHIMMER_END_OFFSET,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_ANIMATION_DURATION_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerAnimation",
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim, translateAnim),
        end = Offset(
            x = translateAnim + SHIMMER_END_OFFSET / 2,
            y = translateAnim + SHIMMER_END_OFFSET / 2,
        ),
    )

    Box(
        modifier = modifier.background(brush = brush),
    )
}

/**
 * Returns a ready-to-use animated shimmer [Brush] for custom shimmer effects.
 *
 * Use this when you need more control — for example applying shimmer to text
 * or a custom-shaped element rather than a rectangular box.
 *
 * Usage:
 * ```kotlin
 * val shimmerBrush = rememberShimmerBrush()
 * Text(
 *     text = "Loading...",
 *     modifier = Modifier.background(shimmerBrush)
 * )
 * ```
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.surfaceVariant,
    )

    val transition = rememberInfiniteTransition(label = "shimmerBrushTransition")
    val translateAnim by transition.animateFloat(
        initialValue = SHIMMER_START_OFFSET,
        targetValue = SHIMMER_END_OFFSET,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_ANIMATION_DURATION_MS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerBrushAnimation",
    )

    return Brush.linearGradient(
        colorStops = arrayOf(
            SHIMMER_GRADIENT_START to shimmerColors[0],
            SHIMMER_GRADIENT_CENTER to shimmerColors[1],
            SHIMMER_GRADIENT_END to shimmerColors[2],
        ),
        start = Offset(translateAnim, translateAnim),
        end = Offset(
            x = translateAnim + SHIMMER_END_OFFSET / 2,
            y = translateAnim + SHIMMER_END_OFFSET / 2,
        ),
    )
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun ShimmerBoxPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.skeletonLineHeightLarge)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(fraction = 0.6f)
                    .height(Dimens.skeletonLineHeightSmall)
                    .clip(MaterialTheme.shapes.small),
            )
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.skeletonLineHeightSmall)
                    .clip(MaterialTheme.shapes.small),
            )
        }
    }
}
