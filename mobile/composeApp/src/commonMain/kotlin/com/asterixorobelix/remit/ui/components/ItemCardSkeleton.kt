package com.asterixorobelix.remit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import kotlinx.coroutines.delay

private const val TITLE_WIDTH_FRACTION = 0.7f
private const val ARTIST_WIDTH_FRACTION = 0.4f
private const val DESC_LINE_1_FRACTION = 1.0f
private const val DESC_LINE_2_FRACTION = 0.9f
private const val DESC_LINE_3_FRACTION = 0.6f
private const val DEFAULT_SKELETON_COUNT = 4
private const val STAGGER_DELAY_MS = 75L

/**
 * Skeleton loading card that mirrors the layout of [ItemCard].
 *
 * Usage — single card:
 * ```kotlin
 * ItemCardSkeleton()
 * ```
 *
 * Usage — full list while loading:
 * ```kotlin
 * if (uiState is Loading) {
 *     ItemListSkeleton(skeletonCount = 5)
 * } else {
 *     ItemList(items = uiState.items, ...)
 * }
 * ```
 */
@Composable
fun ItemCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLevel2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium),
        ) {
            SkeletonHeader()
            Spacer(modifier = Modifier.height(Dimens.spacingCompact))
            SkeletonDescription()
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            SkeletonStatusBadge()
        }
    }
}

/**
 * A lazy list of [ItemCardSkeleton] items with staggered fade-in animations.
 *
 * @param skeletonCount Number of skeleton cards (default 4)
 * @param modifier      Applied to the [LazyColumn]
 */
@Composable
fun ItemListSkeleton(
    modifier: Modifier = Modifier,
    skeletonCount: Int = DEFAULT_SKELETON_COUNT,
) {
    val items = remember(skeletonCount) { (0 until skeletonCount).toList() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        contentPadding = PaddingValues(
            horizontal = Dimens.paddingMedium,
            vertical = Dimens.paddingMedium,
        ),
    ) {
        itemsIndexed(items = items, key = { i, _ -> "skeleton_$i" }) { index, _ ->
            AnimatedSkeletonCard(index = index)
        }
    }
}

@Composable
private fun AnimatedSkeletonCard(index: Int) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * STAGGER_DELAY_MS)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = Dimens.ANIMATION_DURATION_MEDIUM)) +
            slideInVertically(
                animationSpec = tween(durationMillis = Dimens.ANIMATION_DURATION_MEDIUM),
                initialOffsetY = { it / 4 },
            ),
    ) {
        ItemCardSkeleton()
    }
}

@Composable
private fun SkeletonHeader() {
    Column {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(TITLE_WIDTH_FRACTION)
                .height(Dimens.skeletonLineHeightLarge)
                .clip(MaterialTheme.shapes.small),
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShimmerBox(modifier = Modifier.size(Dimens.iconSizeSmall).clip(MaterialTheme.shapes.small))
            Spacer(modifier = Modifier.width(Dimens.spacingSmall))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(ARTIST_WIDTH_FRACTION)
                    .height(Dimens.skeletonLineHeightSmall)
                    .clip(MaterialTheme.shapes.small),
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = Dimens.dividerThickness)
    }
}

@Composable
private fun SkeletonDescription() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingExtraSmall)) {
        SkeletonDescriptionLine(DESC_LINE_1_FRACTION)
        SkeletonDescriptionLine(DESC_LINE_2_FRACTION)
        SkeletonDescriptionLine(DESC_LINE_3_FRACTION)
    }
}

@Composable
private fun SkeletonDescriptionLine(widthFraction: Float) {
    ShimmerBox(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(Dimens.skeletonLineHeightSmall)
            .clip(MaterialTheme.shapes.small),
    )
}

@Composable
private fun SkeletonStatusBadge() {
    Box(
        modifier = Modifier
            .width(Dimens.skeletonBadgeWidth)
            .height(Dimens.skeletonBadgeHeight)
            .clip(MaterialTheme.shapes.small),
    ) {
        ShimmerBox(modifier = Modifier.matchParentSize())
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun ItemCardSkeletonPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            ItemCardSkeleton()
            ItemCardSkeleton()
        }
    }
}
