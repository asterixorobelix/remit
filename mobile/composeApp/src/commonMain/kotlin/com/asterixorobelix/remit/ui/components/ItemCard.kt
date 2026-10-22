package com.asterixorobelix.remit.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_add_favorite
import com.asterixorobelix.remit.generated.resources.cd_remove_favorite
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * A generic, richly-featured content card extracted from the AfrikaBurn companion app.
 *
 * Supports an optional colour-coded left indicator bar, a type badge, title/subtitle row
 * with a favourite toggle, a truncated description, an optional location row, and an
 * optional status badge. All fields are plain strings so the card is completely
 * domain-agnostic.
 *
 * Usage:
 * ```kotlin
 * ItemCard(
 *     title = "Dust Storm Collective",
 *     description = "An immersive art installation exploring the relationship between humans and the desert.",
 *     subtitle = "Sarah Johnson",       // e.g. artist / author / owner
 *     typeBadgeLabel = "ART",
 *     typeBadgeColor = MaterialTheme.colorScheme.primary,
 *     location = "7:30 & Esplanade",
 *     statusLabel = "Day Time | Family-friendly",
 *     isFavorite = favorites.contains(item.id),
 *     onFavoriteToggle = { viewModel.toggleFavorite(item.id) },
 *     onClick = { navController.navigate(Routes.detail(item.id)) }
 * )
 * ```
 *
 * To disable the indicator bar, pass `indicatorColor = null` (or omit it).
 * To hide the favourite button, pass `onFavoriteToggle = null` (or omit it).
 *
 * @param title             Primary bold title
 * @param description       Body text (truncated to 2 lines)
 * @param modifier          Applied to the card
 * @param subtitle          Optional secondary line below title (e.g. author, category)
 * @param typeBadgeLabel    Optional small chip label above the title
 * @param typeBadgeColor    Colour of the type badge and left indicator bar
 * @param indicatorColor    Colour of the 4 dp left indicator bar; null = no bar
 * @param location          Optional location string shown with a pin icon
 * @param statusLabel       Optional status badge shown at the bottom of the card
 * @param isFavorite        Whether the item is currently favourited
 * @param onFavoriteToggle  Callback when the heart button is tapped; null = button hidden
 * @param onClick           Callback when the card body is tapped; null = card not clickable
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
fun ItemCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    typeBadgeLabel: String? = null,
    typeBadgeColor: Color? = null,
    indicatorColor: Color? = typeBadgeColor,
    location: String? = null,
    statusLabel: String? = null,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animatedScale(isPressed = isPressed),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLevel2),
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            // Left colour indicator bar
            if (indicatorColor != null) {
                Box(
                    modifier = Modifier
                        .width(Dimens.categoryIndicatorWidth)
                        .fillMaxHeight()
                        .background(indicatorColor),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Dimens.cardContentPaddingHorizontal,
                        vertical = Dimens.cardContentPaddingVertical,
                    ),
            ) {
                // Type badge (optional)
                if (typeBadgeLabel != null && typeBadgeColor != null) {
                    ItemTypeBadge(label = typeBadgeLabel, color = typeBadgeColor)
                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                }

                // Title + subtitle + favourite button
                ItemCardHeader(
                    title = title,
                    subtitle = subtitle,
                    isFavorite = isFavorite,
                    onFavoriteToggle = onFavoriteToggle,
                )

                Spacer(modifier = Modifier.height(Dimens.spacingCompact))

                // Description
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                // Location (optional)
                if (!location.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(Dimens.iconSizeSmall),
                        )
                        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // Status badge (optional)
                if (!statusLabel.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(Dimens.spacingMedium))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(
                                horizontal = Dimens.paddingSmall,
                                vertical = Dimens.paddingExtraSmall,
                            ),
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun ItemTypeBadge(label: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(
                horizontal = Dimens.paddingSmall,
                vertical = Dimens.paddingExtraSmall,
            ),
        )
    }
}

@Composable
private fun ItemCardHeader(
    title: String,
    subtitle: String?,
    isFavorite: Boolean,
    onFavoriteToggle: (() -> Unit)?,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (onFavoriteToggle != null) {
                FavoriteButton(isFavorite = isFavorite, onToggle = onFavoriteToggle)
            }
        }

        if (!subtitle.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(Dimens.iconSizeSmall),
                )
                Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = Dimens.dividerThickness,
            )
        }
    }
}

@Composable
private fun FavoriteButton(isFavorite: Boolean, onToggle: () -> Unit) {
    val tint by animateColorAsState(
        targetValue = if (isFavorite) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "favoriteTint",
    )

    IconButton(
        onClick = onToggle,
        modifier = Modifier.size(Dimens.touchTargetMinimum),
    ) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) Res.string.cd_remove_favorite else Res.string.cd_add_favorite,
            ),
            tint = tint,
            modifier = Modifier.size(Dimens.iconSizeMedium),
        )
    }
}

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun ItemCardPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        ) {
            // Full featured card
            ItemCard(
                title = "Dust Storm Collective",
                description =
                "An immersive art installation exploring the relationship " +
                    "between humans and the desert environment.",
                subtitle = "Sarah Johnson",
                typeBadgeLabel = "ART",
                typeBadgeColor = MaterialTheme.colorScheme.primary,
                location = "7:30 & Esplanade",
                statusLabel = "Day Time | Family-friendly",
                isFavorite = true,
                onFavoriteToggle = {},
                onClick = {},
            )

            // Minimal card (no indicator, no badge, no favourite)
            ItemCard(
                title = "Simple Item",
                description = "A minimal card with just title and description.",
            )
        }
    }
}
