package com.example.myproject.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens

/**
 * A list item that can be swiped from right to left to trigger a delete action.
 *
 * The swipe reveals a red delete background with a trash icon. On full swipe
 * the [onDelete] callback is invoked. The item snaps back if the swipe is
 * released before reaching the threshold (does NOT auto-dismiss).
 *
 * If you want a confirmation dialog before deleting, show it inside [onDelete]
 * and only actually delete after the user confirms.
 *
 * Usage:
 * ```kotlin
 * LazyColumn {
 *     items(items, key = { it.id }) { item ->
 *         SwipeableListItem(
 *             onDelete = { viewModel.onDeleteRequested(item) }
 *         ) {
 *             MyItemCard(item = item)
 *         }
 *     }
 * }
 * ```
 *
 * @param onDelete Called when the user completes a full swipe-to-delete gesture
 * @param modifier Optional modifier for the outer container
 * @param content The list item content to display
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableListItem(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
            }
            // Return false to snap back — let the caller control actual removal
            false
        },
    )

    // Reset state after each swipe so the item is ready for the next gesture
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = { SwipeDeleteBackground() },
        content = { content() },
    )
}

@Composable
private fun SwipeDeleteBackground() {
    val color by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.errorContainer,
        label = "swipeBackground",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = Dimens.paddingLarge),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(Dimens.iconSizeLarge),
        )
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun SwipeableListItemPreview() {
    AppTheme {
        SwipeableListItem(onDelete = {}) {
            AppCard(modifier = Modifier.padding(Dimens.paddingSmall)) {
                Text(
                    text = "Swipe me left to delete",
                    modifier = Modifier.padding(Dimens.paddingMedium),
                )
            }
        }
    }
}
