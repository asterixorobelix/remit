package com.example.myproject.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.cd_page_indicator
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

private const val UNSELECTED_ALPHA = 0.38f
private const val SELECTED_SCALE = 1f
private const val UNSELECTED_SCALE = 0.75f

/**
 * Animated dot row page indicator for use with [HorizontalPager].
 *
 * Features smooth scale, size and colour transitions between pages,
 * including during in-progress swipe gestures via [pageOffset].
 *
 * Usage with Compose Pager:
 * ```kotlin
 * val pagerState = rememberPagerState(pageCount = { pages.size })
 *
 * HorizontalPager(state = pagerState) { page -> ... }
 *
 * PageIndicator(
 *     currentPage = pagerState.currentPage,
 *     totalPages = pages.size,
 *     pageOffset = pagerState.currentPageOffsetFraction
 * )
 * ```
 *
 * @param currentPage Zero-based index of the currently displayed page
 * @param totalPages  Total number of pages
 * @param modifier    Applied to the indicator row
 * @param pageOffset  Fractional swipe progress (0f–1f) from [PagerState.currentPageOffsetFraction].
 *                    Defaults to 0f (no transition animation needed when set programmatically).
 */
@Composable
fun PageIndicator(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier,
    pageOffset: Float = 0f,
) {
    val pageDescription = stringResource(Res.string.cd_page_indicator, currentPage + 1, totalPages)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.paddingSmall)
            .semantics {
                contentDescription = pageDescription
            },
        horizontalArrangement = Arrangement.spacedBy(
            Dimens.indicatorSpacing,
            Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            AnimatedIndicatorDot(
                index = index,
                currentPage = currentPage,
                pageOffset = pageOffset,
                totalPages = totalPages,
            )
        }
    }
}

@Composable
private fun AnimatedIndicatorDot(
    index: Int,
    currentPage: Int,
    pageOffset: Float,
    totalPages: Int,
) {
    val isSelected = index == currentPage
    val isNextPage = index == currentPage + 1 && currentPage < totalPages - 1

    // Interpolate during live swipe gesture
    val selectionProgress = when {
        isSelected -> 1f - pageOffset
        isNextPage -> pageOffset
        else -> 0f
    }

    val targetScale = UNSELECTED_SCALE + (SELECTED_SCALE - UNSELECTED_SCALE) * selectionProgress

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorScale",
    )

    val size by animateDpAsState(
        targetValue = if (selectionProgress > 0.5f) {
            Dimens.indicatorDotSizeLarge
        } else {
            Dimens.indicatorDotSizeSmall
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorSize",
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = UNSELECTED_ALPHA)

    val color by animateColorAsState(
        targetValue = if (selectionProgress > 0.5f) primaryColor else unselectedColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "indicatorColor",
    )

    Box(
        modifier = Modifier
            .size(Dimens.indicatorDotSizeLarge)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun PageIndicatorPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
        ) {
            Text(
                text = "First page (index 0)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PageIndicator(currentPage = 0, totalPages = 4)

            Text(
                text = "Middle page (index 2)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PageIndicator(currentPage = 2, totalPages = 4)

            Text(
                text = "Mid-swipe (index 1, offset 0.5f)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PageIndicator(currentPage = 1, totalPages = 4, pageOffset = 0.5f)
        }
    }
}
