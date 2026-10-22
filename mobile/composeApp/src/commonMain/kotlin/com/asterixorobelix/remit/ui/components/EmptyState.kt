package com.asterixorobelix.remit.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.message_empty_state
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Empty state component for when there is no content to display.
 *
 * Follows Material Design 3 guidelines with subtle emphasis
 * to indicate an empty but non-error state.
 *
 * @param modifier Modifier to be applied to the container
 * @param icon The icon to display. Defaults to Info icon.
 * @param title The main title text
 * @param description Optional description text below the title
 * @param actionLabel Optional label for the action button
 * @param onAction Optional callback for the action button
 */
@Suppress("LongParameterList")
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    title: String = stringResource(Res.string.message_empty_state),
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) +
            scaleIn(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), initialScale = 0.9f),
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(Dimens.paddingExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSizeDisplay),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            if (description != null) {
                Spacer(modifier = Modifier.height(Dimens.spacingSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(Dimens.spacingLarge))
                Button(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

/**
 * Full-screen empty state that centers the content.
 *
 * Use this when empty state is the primary content of a screen.
 *
 * @param modifier Modifier to be applied to the container
 * @param icon The icon to display
 * @param title The main title text
 * @param description Optional description text
 * @param actionLabel Optional label for the action button
 * @param onAction Optional callback for the action button
 */
@Suppress("LongParameterList")
@Composable
fun FullScreenEmpty(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    title: String = stringResource(Res.string.message_empty_state),
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        EmptyState(
            icon = icon,
            title = title,
            description = description,
            actionLabel = actionLabel,
            onAction = onAction,
        )
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun EmptyStatePreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingExtraLarge),
        ) {
            // Simple empty state
            EmptyState(
                title = "No items found",
            )

            // Empty state with description and action
            EmptyState(
                title = "No users yet",
                description = "Create your first user to get started",
                actionLabel = "Create User",
                onAction = { },
            )
        }
    }
}
