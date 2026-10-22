package com.asterixorobelix.remit.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.button_cancel
import com.asterixorobelix.remit.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

/**
 * A reusable confirmation dialog for irreversible actions (delete, sign out, etc.).
 *
 * Shows a title, message, and two buttons: a destructive confirm and a neutral cancel.
 * The confirm button uses the error color scheme to visually signal danger.
 *
 * Usage:
 * ```kotlin
 * var showDeleteDialog by remember { mutableStateOf(false) }
 *
 * if (showDeleteDialog) {
 *     ConfirmationDialog(
 *         title = "Delete item?",
 *         message = "This cannot be undone.",
 *         confirmLabel = "Delete",
 *         onConfirm = {
 *             showDeleteDialog = false
 *             viewModel.onDeleteConfirmed(item)
 *         },
 *         onDismiss = { showDeleteDialog = false },
 *     )
 * }
 * ```
 *
 * Pair with [SwipeableListItem] for swipe-to-delete patterns:
 * ```kotlin
 * SwipeableListItem(onDelete = { showDeleteDialog = true }) {
 *     MyItemCard(item)
 * }
 * ```
 *
 * @param title Dialog headline
 * @param message Supporting text explaining the consequences
 * @param confirmLabel Label for the destructive confirm button (e.g. "Delete", "Remove")
 * @param dismissLabel Label for the cancel button (default "Cancel")
 * @param onConfirm Called when the user taps the confirm button
 * @param onDismiss Called when the user taps cancel or dismisses the dialog
 * @param modifier Optional modifier for the dialog container
 */
@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = stringResource(Res.string.button_cancel),
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirmLabel,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = dismissLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun ConfirmationDialogPreview() {
    AppTheme {
        ConfirmationDialog(
            title = "Delete item?",
            message = "This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
