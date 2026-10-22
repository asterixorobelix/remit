package com.asterixorobelix.remit.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import com.asterixorobelix.remit.ui.theme.WarningDark
import com.asterixorobelix.remit.ui.theme.WarningLight

/** Status level for a [StatusBadge]. */
enum class BadgeStatus {
    /** Positive outcome — uses tertiary (green) color. */
    Success,

    /** Cautionary outcome — uses amber/warning color. */
    Warning,

    /** Negative outcome — uses error (red) color. */
    Error,
}

private const val DARK_LUMINANCE_THRESHOLD = 0.5f

/**
 * Pill-shaped status badge with three severity levels.
 *
 * Displays a colored icon + uppercase label in a rounded pill container.
 * Colors adapt to light/dark theme automatically.
 *
 * Usage:
 * ```kotlin
 * // Compliance / validation result
 * StatusBadge(status = BadgeStatus.Success, label = "Verified")
 * StatusBadge(status = BadgeStatus.Warning, label = "Review Needed")
 * StatusBadge(status = BadgeStatus.Error,   label = "Action Required")
 *
 * // Order / payment status
 * StatusBadge(status = BadgeStatus.Success, label = "Paid")
 * StatusBadge(status = BadgeStatus.Warning, label = "Pending")
 * StatusBadge(status = BadgeStatus.Error,   label = "Failed")
 * ```
 *
 * @param status The severity level — drives icon and color
 * @param label Text shown inside the badge (displayed as uppercase)
 * @param modifier Optional modifier for sizing/positioning
 */
@Composable
fun StatusBadge(
    status: BadgeStatus,
    label: String,
    modifier: Modifier = Modifier,
) {
    // Follows the app's theme choice (Settings), not only the system setting.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < DARK_LUMINANCE_THRESHOLD

    val (icon, foreground, background) = when (status) {
        BadgeStatus.Success -> Triple(
            Icons.Filled.CheckCircle,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
        )

        BadgeStatus.Warning -> Triple(
            Icons.Filled.Warning,
            if (isDark) WarningDark else WarningLight,
            WarningLight.copy(alpha = if (isDark) 0.15f else 0.12f),
        )

        BadgeStatus.Error -> Triple(
            Icons.Filled.Error,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer,
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.cornerRadiusFull))
            .background(background)
            .padding(
                horizontal = Dimens.paddingSmall,
                vertical = Dimens.spacingExtraSmall,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(Dimens.iconSizeSmall),
        )
        Spacer(modifier = Modifier.width(Dimens.spacingExtraSmall))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun StatusBadgePreview() {
    AppTheme {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(Dimens.paddingMedium),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Dimens.spacingSmall),
        ) {
            StatusBadge(status = BadgeStatus.Success, label = "Verified")
            StatusBadge(status = BadgeStatus.Warning, label = "Review Needed")
            StatusBadge(status = BadgeStatus.Error, label = "Action Required")
        }
    }
}
