package com.example.myproject.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myproject.ui.theme.Dimens
import com.example.myproject.ui.theme.pressedScale

/**
 * App card component — uses Surface with subtle border instead of ElevatedCard.
 *
 * This avoids the generic "Material shadow card" look. Uses:
 * - 16dp corner radius (MaterialTheme.shapes.small, overridden from MD3 default 8dp)
 * - 1dp border at 12% opacity (subtle, modern)
 * - No shadow elevation (flat, clean)
 * - Press-scale feedback on interactive cards
 *
 * Usage:
 * ```kotlin
 * AppCard(onClick = { navigateToDetail() }) {
 *     Text("Card content")
 * }
 * ```
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardModifier = if (onClick != null) modifier.pressedScale() else modifier

    Surface(
        modifier = cardModifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = Dimens.borderWidth,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        onClick = onClick ?: {},
        enabled = onClick != null,
        content = { androidx.compose.foundation.layout.Column(content = content) },
    )
}
