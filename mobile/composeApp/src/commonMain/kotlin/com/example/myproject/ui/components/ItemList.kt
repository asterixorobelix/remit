package com.example.myproject.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.myproject.ui.theme.Dimens
import kotlinx.coroutines.delay

private const val MAX_ANIMATED_ITEMS = 10

/**
 * A lazy, animated list of [ItemCard] composables.
 *
 * Each card enters with a staggered `fadeIn + slideInVertically` animation
 * (capped at the first [MAX_ANIMATED_ITEMS] items for performance).
 * Pairs naturally with [ItemListSkeleton] for a loading → content transition.
 *
 * The list is domain-agnostic: you supply an [items] list of [ItemData] value objects
 * and optional callbacks. Build [ItemData] from any domain model in the call site.
 *
 * Usage:
 * ```kotlin
 * val items = products.map { p ->
 *     ItemData(
 *         key = p.id,
 *         title = p.name,
 *         description = p.summary,
 *         subtitle = p.brand,
 *         typeBadgeLabel = p.category.name,
 *         typeBadgeColor = categoryColor(p.category),
 *         location = p.storeName,
 *         statusLabel = if (p.inStock) "In Stock" else "Out of Stock",
 *         isFavorite = p.id in favorites
 *     )
 * }
 *
 * ItemList(
 *     items = items,
 *     onFavoriteToggle = { key -> viewModel.toggleFavorite(key) },
 *     onItemClick = { key -> navController.navigate(Routes.Detail(key)) }
 * )
 * ```
 *
 * @param items           Data to display — see [ItemData]
 * @param modifier        Applied to the [LazyColumn]
 * @param listState       Optional external [LazyListState] for scroll control
 * @param onFavoriteToggle Called with the item's [ItemData.key] when the heart is tapped
 * @param onItemClick     Called with the item's [ItemData.key] when the card is tapped
 */
@Composable
fun ItemList(
    items: List<ItemData>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onFavoriteToggle: ((key: String) -> Unit)? = null,
    onItemClick: ((key: String) -> Unit)? = null,
) {
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        contentPadding = PaddingValues(
            horizontal = Dimens.paddingMedium,
            vertical = Dimens.paddingMedium,
        ),
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.key },
        ) { index, item ->
            AnimatedItemCard(
                item = item,
                index = index,
                onFavoriteToggle = onFavoriteToggle?.let { cb -> { cb(item.key) } },
                onItemClick = onItemClick?.let { cb -> { cb(item.key) } },
            )
        }
    }
}

@Composable
private fun AnimatedItemCard(
    item: ItemData,
    index: Int,
    onFavoriteToggle: (() -> Unit)?,
    onItemClick: (() -> Unit)?,
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(item.key) {
        val delayMs = (index.coerceAtMost(MAX_ANIMATED_ITEMS) * Dimens.STAGGER_DELAY_PER_ITEM).toLong()
        delay(delayMs)
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
        ItemCard(
            title = item.title,
            description = item.description,
            subtitle = item.subtitle,
            typeBadgeLabel = item.typeBadgeLabel,
            typeBadgeColor = item.typeBadgeColor,
            indicatorColor = item.indicatorColor ?: item.typeBadgeColor,
            location = item.location,
            statusLabel = item.statusLabel,
            isFavorite = item.isFavorite,
            onFavoriteToggle = onFavoriteToggle,
            onClick = onItemClick,
        )
    }
}

/**
 * Plain data holder for [ItemList] / [ItemCard].
 *
 * Convert your domain models to [ItemData] at the call site so the component
 * stays completely decoupled from your data layer.
 *
 * @param key             Unique stable key used as the LazyColumn item key
 * @param title           Card headline
 * @param description     Body text (truncated to 2 lines by [ItemCard])
 * @param subtitle        Optional secondary line (e.g. author, brand)
 * @param typeBadgeLabel  Optional small chip label above the title
 * @param typeBadgeColor  Colour for the type badge and default indicator bar
 * @param indicatorColor  Override colour for the left indicator bar; defaults to [typeBadgeColor]
 * @param location        Optional location string
 * @param statusLabel     Optional status badge text
 * @param isFavorite      Whether the item is currently favourited
 */
data class ItemData(
    val key: String,
    val title: String,
    val description: String,
    val subtitle: String? = null,
    val typeBadgeLabel: String? = null,
    val typeBadgeColor: Color? = null,
    val indicatorColor: Color? = null,
    val location: String? = null,
    val statusLabel: String? = null,
    val isFavorite: Boolean = false,
)
