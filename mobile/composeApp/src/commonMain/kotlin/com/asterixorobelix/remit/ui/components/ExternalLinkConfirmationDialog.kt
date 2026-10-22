package com.asterixorobelix.remit.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.button_cancel
import com.asterixorobelix.remit.generated.resources.external_link_message
import com.asterixorobelix.remit.generated.resources.external_link_open
import com.asterixorobelix.remit.generated.resources.external_link_title
import org.jetbrains.compose.resources.stringResource

/*
 * Confirmation dialog shown before opening any external URL.
 *
 * Prevents accidental navigation to external sites by asking the user to
 * confirm first. Especially useful in apps that embed important content
 * in-app and want to make external transitions deliberate.
 *
 * Two usage patterns are provided:
 * 1. [ExternalLinkConfirmationDialog] — raw dialog, you control show/hide state
 * 2. [ExternalLinkButton] — convenience wrapper that manages state internally
 *
 * Usage (pattern 1 — manual state):
 * ```kotlin
 * var showDialog by remember { mutableStateOf(false) }
 *
 * Button(onClick = { showDialog = true }) { Text("Visit website") }
 *
 * if (showDialog) {
 *     ExternalLinkConfirmationDialog(
 *         url = "https://example.com",
 *         onDismiss = { showDialog = false }
 *     )
 * }
 * ```
 *
 * Usage (pattern 2 — self-contained):
 * ```kotlin
 * ExternalLinkButton(
 *     url = "https://example.com",
 *     label = "Visit website"
 * )
 * ```
 */

/**
 * An [AlertDialog] that asks the user to confirm before opening an external URL.
 *
 * On confirmation, the URL is opened via [LocalUriHandler].
 *
 * @param url           The URL to open when confirmed
 * @param onDismiss     Called when the dialog is dismissed (cancel or confirm)
 * @param title         Dialog title (default: "Open External Link")
 * @param message       Dialog body text (default: generic safety message)
 * @param confirmLabel  Label for the confirm button (default: "Open")
 * @param cancelLabel   Label for the cancel button (default: "Cancel")
 */
@Suppress("LongParameterList")
@Composable
fun ExternalLinkConfirmationDialog(
    url: String,
    onDismiss: () -> Unit,
    title: String = stringResource(Res.string.external_link_title),
    message: String = stringResource(Res.string.external_link_message),
    confirmLabel: String = stringResource(Res.string.external_link_open),
    cancelLabel: String = stringResource(Res.string.button_cancel),
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    uriHandler.openUri(url)
                    onDismiss()
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
    )
}

/**
 * A convenience composable that shows a [Button] which triggers an
 * [ExternalLinkConfirmationDialog] before opening the given [url].
 *
 * Manages dialog state internally so no hoisting is needed.
 *
 * Usage:
 * ```kotlin
 * ExternalLinkButton(
 *     url = "https://example.com",
 *     label = "Learn more"
 * )
 * ```
 *
 * @param url           The URL to open
 * @param label         Button text
 * @param modifier      Modifier applied to the button
 * @param dialogTitle   Optional override for the dialog title
 * @param dialogMessage Optional override for the dialog body
 */
@Suppress("LongParameterList")
@Composable
fun ExternalLinkButton(
    url: String,
    label: String,
    modifier: Modifier = Modifier,
    dialogTitle: String = stringResource(Res.string.external_link_title),
    dialogMessage: String = stringResource(Res.string.external_link_message),
) {
    var showDialog by remember { mutableStateOf(false) }

    Button(onClick = { showDialog = true }, modifier = modifier) {
        Text(label)
    }

    if (showDialog) {
        ExternalLinkConfirmationDialog(
            url = url,
            onDismiss = { showDialog = false },
            title = dialogTitle,
            message = dialogMessage,
        )
    }
}
