package com.example.myproject.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.cd_state_not_selected
import com.example.myproject.generated.resources.cd_state_selected
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * A generic, horizontally scrollable filter chip row.
 *
 * Each [FilterOption] carries its own label, selection state and toggle callback,
 * so the list can represent any domain — time slots, categories, tags, statuses, etc.
 *
 * Features:
 * - Animated colour transition on selection (150 ms)
 * - Bouncy scale feedback via [animateSelectionScale]
 * - Accessibility live region so screen readers announce selection changes
 *
 * Usage:
 * ```kotlin
 * // 1. Define your filter state
 * enum class Status { ALL, ACTIVE, ARCHIVED }
 * var selected by remember { mutableStateOf(Status.ALL) }
 *
 * // 2. Build the chip list
 * val options = Status.entries.map { status ->
 *     FilterOption(
 *         label = status.name.lowercase().replaceFirstChar { it.uppercase() },
 *         isSelected = selected == status,
 *         onToggle = { selected = status }
 *     )
 * }
 *
 * // 3. Render
 * FilterChips(options = options)
 * ```
 *
 * For multi-select:
 * ```kotlin
 * var selectedTags by remember { mutableStateOf(setOf<String>()) }
 *
 * val tagOptions = allTags.map { tag ->
 *     FilterOption(
 *         label = tag,
 *         isSelected = tag in selectedTags,
 *         onToggle = {
 *             selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
 *         }
 *     )
 * }
 *
 * FilterChips(options = tagOptions)
 * ```
 *
 * @param options   List of [FilterOption] items to render
 * @param modifier  Applied to the outer [LazyRow]
 */
@Composable
fun FilterChips(
    options: List<FilterOption>,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.paddingMedium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(options, key = { it.label }) { option ->
            FilterChipItem(option = option)
        }
    }
}

/**
 * A single filter option rendered as a [FilterChip] with animated colours and scale.
 *
 * Can also be used standalone outside [FilterChips] when you need more layout control.
 *
 * Usage:
 * ```kotlin
 * Row {
 *     FilterChipItem(FilterOption("All", isSelected = true, onToggle = {}))
 *     FilterChipItem(FilterOption("Active", isSelected = false, onToggle = {}))
 * }
 * ```
 */
@Composable
fun FilterChipItem(
    option: FilterOption,
    modifier: Modifier = Modifier,
) {
    val scale = animateSelectionScale(isSelected = option.isSelected)
    val selectionDescription = stringResource(
        if (option.isSelected) Res.string.cd_state_selected else Res.string.cd_state_not_selected,
    )

    val containerColor by animateColorAsState(
        targetValue = if (option.isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = shortDurationTween(),
        label = "chipContainerColor",
    )

    val labelColor by animateColorAsState(
        targetValue = if (option.isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = shortDurationTween(),
        label = "chipLabelColor",
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
    ) {
        FilterChip(
            onClick = option.onToggle,
            label = {
                Text(
                    text = option.label,
                    style = MaterialTheme.typography.labelMedium,
                )
            },
            selected = option.isSelected,
            modifier = Modifier.semantics {
                stateDescription = selectionDescription
                liveRegion = LiveRegionMode.Polite
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = containerColor,
                selectedLabelColor = labelColor,
                containerColor = containerColor,
                labelColor = labelColor,
            ),
            border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = option.isSelected,
                borderColor = MaterialTheme.colorScheme.outline,
                selectedBorderColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

/**
 * Data model for a single filter chip.
 *
 * @param label      Displayed text
 * @param isSelected Whether this chip is currently active
 * @param onToggle   Called when the chip is tapped
 */
data class FilterOption(
    val label: String,
    val isSelected: Boolean,
    val onToggle: () -> Unit,
)

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun FilterChipsPreview() {
    AppTheme {
        val options = listOf(
            FilterOption("All", isSelected = true, onToggle = {}),
            FilterOption("Active", isSelected = false, onToggle = {}),
            FilterOption("Archived", isSelected = false, onToggle = {}),
            FilterOption("Favourites", isSelected = false, onToggle = {}),
        )
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = Dimens.paddingSmall),
        ) {
            FilterChips(options = options)
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun FilterChipItemPreview() {
    AppTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            FilterChipItem(FilterOption("Selected", isSelected = true, onToggle = {}))
            FilterChipItem(FilterOption("Unselected", isSelected = false, onToggle = {}))
        }
    }
}
