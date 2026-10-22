package com.example.myproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.button_retry
import com.example.myproject.generated.resources.message_error_generic
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Error state component displaying an error icon, message, and optional retry action.
 *
 * Follows Material Design 3 guidelines with proper error coloring
 * and accessibility support.
 *
 * @param modifier Modifier to be applied to the container
 * @param title Optional title for the error state
 * @param message The error message to display
 * @param onRetry Optional callback for retry action. If null, no button is shown.
 */
@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String = stringResource(Res.string.message_error_generic),
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Dimens.paddingExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Warning,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeDisplay),
            tint = MaterialTheme.colorScheme.error,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingMedium))

        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        }

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        if (onRetry != null) {
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Button(onClick = onRetry) {
                Text(
                    text = stringResource(Res.string.button_retry),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * Full-screen error state that centers the error message.
 *
 * Use this when the error is the primary state of a screen.
 *
 * @param modifier Modifier to be applied to the container
 * @param title Optional title for the error state
 * @param message The error message to display
 * @param onRetry Optional callback for retry action
 */
@Composable
fun FullScreenError(
    modifier: Modifier = Modifier,
    title: String? = null,
    message: String = stringResource(Res.string.message_error_generic),
    onRetry: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        ErrorState(
            title = title,
            message = message,
            onRetry = onRetry,
        )
    }
}

/**
 * Inline error message for form validation or minor errors.
 *
 * Displays a compact error message with error styling.
 *
 * @param message The error message to display
 * @param modifier Modifier to be applied to the text
 */
@Composable
fun InlineError(
    message: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = message,
        modifier = modifier.padding(
            horizontal = Dimens.paddingMedium,
            vertical = Dimens.paddingSmall,
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
    )
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun ErrorStatePreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
        ) {
            // Error state with retry
            ErrorState(
                title = "Connection Error",
                message = "Unable to connect to the server. Please check your internet connection.",
                onRetry = { },
            )

            // Simple error without retry
            ErrorState(
                message = "Something went wrong",
            )

            // Inline error
            InlineError(message = "Invalid email address")
        }
    }
}
