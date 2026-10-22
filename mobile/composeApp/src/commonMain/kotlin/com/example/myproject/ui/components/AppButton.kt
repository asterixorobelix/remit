package com.example.myproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens
import com.example.myproject.ui.theme.pressedScale

/**
 * Primary button following Material Design 3 filled button style.
 *
 * Features:
 * - Proper touch target size (minimum 48dp)
 * - Loading state support
 * - Icon support
 * - Full accessibility support
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier to be applied to the button
 * @param text The button label text
 * @param enabled Whether the button is enabled
 * @param loading Whether to show loading indicator
 * @param icon Optional leading icon
 * @param contentDescription Optional content description for accessibility
 */
@Suppress("LongParameterList")
@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .pressedScale()
            .defaultMinSize(minHeight = Dimens.touchTargetMinimum),
        enabled = enabled && !loading,
        contentPadding = PaddingValues(
            horizontal = Dimens.paddingLarge,
            vertical = Dimens.paddingCompact,
        ),
    ) {
        ButtonContent(
            text = text,
            loading = loading,
            icon = icon,
            contentDescription = contentDescription,
        )
    }
}

/**
 * Secondary button following Material Design 3 outlined button style.
 *
 * Use for secondary actions that need less visual emphasis.
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier to be applied to the button
 * @param text The button label text
 * @param enabled Whether the button is enabled
 * @param loading Whether to show loading indicator
 * @param icon Optional leading icon
 * @param contentDescription Optional content description for accessibility
 */
@Suppress("LongParameterList")
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .pressedScale()
            .defaultMinSize(minHeight = Dimens.touchTargetMinimum),
        enabled = enabled && !loading,
        contentPadding = PaddingValues(
            horizontal = Dimens.paddingLarge,
            vertical = Dimens.paddingCompact,
        ),
    ) {
        ButtonContent(
            text = text,
            loading = loading,
            icon = icon,
            contentDescription = contentDescription,
            isPrimary = false,
        )
    }
}

/**
 * Text button following Material Design 3 text button style.
 *
 * Use for low-emphasis actions, typically in dialogs or inline.
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier to be applied to the button
 * @param text The button label text
 * @param enabled Whether the button is enabled
 */
@Composable
fun AppTextButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Dimens.touchTargetMinimum),
        enabled = enabled,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

/**
 * Destructive button for delete or dangerous actions.
 *
 * Uses error colors to indicate destructive action.
 *
 * @param onClick Callback when button is clicked
 * @param modifier Modifier to be applied to the button
 * @param text The button label text
 * @param enabled Whether the button is enabled
 * @param loading Whether to show loading indicator
 */
@Composable
fun DestructiveButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .pressedScale()
            .defaultMinSize(minHeight = Dimens.touchTargetMinimum),
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        ),
        contentPadding = PaddingValues(
            horizontal = Dimens.paddingLarge,
            vertical = Dimens.paddingCompact,
        ),
    ) {
        ButtonContent(
            text = text,
            loading = loading,
        )
    }
}

@Composable
private fun RowScope.ButtonContent(
    text: String,
    loading: Boolean,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    isPrimary: Boolean = true,
) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(Dimens.iconSizeMedium),
            color = if (isPrimary) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
            strokeWidth = Dimens.borderWidthThick,
        )
        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
    } else if (icon != null) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(Dimens.iconSizeCompact),
        )
        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
    )
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun AppButtonPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Primary button
            PrimaryButton(onClick = { }, text = "Primary Button")

            // Primary button with icon
            PrimaryButton(
                onClick = { },
                text = "Add Item",
                icon = Icons.Default.Add,
            )

            // Primary button loading
            PrimaryButton(onClick = { }, text = "Loading", loading = true)

            // Primary button disabled
            PrimaryButton(onClick = { }, text = "Disabled", enabled = false)

            // Secondary button
            SecondaryButton(onClick = { }, text = "Secondary Button")

            // Text button
            AppTextButton(onClick = { }, text = "Text Button")

            // Destructive button
            DestructiveButton(onClick = { }, text = "Delete")
        }
    }
}
