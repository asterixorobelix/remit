package com.asterixorobelix.remit.presentation.map

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_center_location
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Floating action button to center map on user's location.
 *
 * Features a bounce animation on press for tactile feedback.
 * Uses Material Design 3 styling with appropriate colors for
 * enabled and disabled states.
 *
 * @param onClick Called when button is tapped
 * @param modifier Optional modifier
 * @param enabled Whether the button is enabled (location available)
 */
@Composable
fun MyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "fab_scale",
    )

    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        interactionSource = interactionSource,
        containerColor = if (enabled) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        contentColor = if (enabled) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = Dimens.elevationLevel4,
            pressedElevation = Dimens.elevationLevel2,
        ),
    ) {
        Icon(
            imageVector = Icons.Default.MyLocation,
            contentDescription = stringResource(Res.string.cd_center_location),
            modifier = Modifier.size(Dimens.iconSizeMedium),
        )
    }
}
