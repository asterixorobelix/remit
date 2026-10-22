package com.asterixorobelix.remit.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.countdown_days
import com.asterixorobelix.remit.generated.resources.countdown_hours
import com.asterixorobelix.remit.generated.resources.countdown_minutes
import com.asterixorobelix.remit.generated.resources.countdown_seconds
import com.asterixorobelix.remit.generated.resources.countdown_separator
import com.asterixorobelix.remit.generated.resources.countdown_starts_in
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import com.asterixorobelix.remit.util.currentTimeMillis
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val COUNTDOWN_TICK_MS = 1_000L
private const val SECS_PER_MIN = 60L
private const val SECS_PER_HOUR = 3_600L
private const val SECS_PER_DAY = 86_400L
private const val PULSE_DURATION_MS = 2_000
private const val PREVIEW_BASE_TIMESTAMP_MS = 1_745_000_000_000L
private const val PREVIEW_HOURS = 48
private const val MILLIS_PER_HOUR = 3_600_000L

/**
 * A live countdown card that ticks every second towards a [targetEpochMs] timestamp.
 *
 * The card is automatically hidden once the target time has passed.
 * An optional pulsing icon provides visual energy to the countdown.
 *
 * Usage — count down to a known epoch millis:
 * ```kotlin
 * CountdownCard(
 *     targetEpochMs = eventStartMs,
 *     title = "EVENT STARTS IN",
 *     dateLabel = "Saturday 27 April 2026"
 * )
 * ```
 *
 * Usage — provide epoch millis from a kotlinx-datetime LocalDate:
 * ```kotlin
 * val targetMs = remember {
 *     LocalDate(2026, 4, 27)
 *         .atStartOfDayIn(TimeZone.of("Africa/Johannesburg"))
 *         .toEpochMilliseconds()
 * }
 * CountdownCard(targetEpochMs = targetMs, title = "BURN STARTS IN")
 * ```
 *
 * @param targetEpochMs  Unix epoch milliseconds to count down to
 * @param modifier       Applied to the outer [Card]
 * @param title          Header label above the digits (default "STARTS IN")
 * @param dateLabel      Optional secondary label below the digits (e.g. formatted date)
 * @param icon           Icon shown above the title (default: Timer)
 */
@Suppress("LongParameterList")
@Composable
fun CountdownCard(
    targetEpochMs: Long,
    modifier: Modifier = Modifier,
    title: String = stringResource(Res.string.countdown_starts_in),
    dateLabel: String? = null,
    icon: ImageVector = Icons.Default.Timer,
) {
    data class CountdownTime(val days: Long, val hours: Long, val minutes: Long, val seconds: Long)

    var countdown by remember { mutableStateOf(CountdownTime(0, 0, 0, 0)) }
    var isPast by remember { mutableStateOf(false) }

    LaunchedEffect(targetEpochMs) {
        while (true) {
            val nowMs = currentTimeMillis()
            val remainingMs = targetEpochMs - nowMs
            val totalSeconds = remainingMs / COUNTDOWN_TICK_MS

            if (totalSeconds <= 0) {
                isPast = true
                break
            }

            countdown = CountdownTime(
                days = totalSeconds / SECS_PER_DAY,
                hours = (totalSeconds % SECS_PER_DAY) / SECS_PER_HOUR,
                minutes = (totalSeconds % SECS_PER_HOUR) / SECS_PER_MIN,
                seconds = totalSeconds % SECS_PER_MIN,
            )
            delay(COUNTDOWN_TICK_MS)
        }
    }

    if (isPast) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = Dimens.elevationLevel2),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingMedium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingCompact),
        ) {
            PulsingIcon(icon = icon)

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            CountdownDigitRow(
                days = countdown.days,
                hours = countdown.hours,
                minutes = countdown.minutes,
                seconds = countdown.seconds,
            )

            if (dateLabel != null) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Private sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun PulsingIcon(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "iconPulse")
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(PULSE_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .size(Dimens.iconSizeExtraLarge)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        tint = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun CountdownDigitRow(days: Long, hours: Long, minutes: Long, seconds: Long) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CountdownUnit(value = days, label = stringResource(Res.string.countdown_days))
        CountdownSeparator()
        CountdownUnit(value = hours, label = stringResource(Res.string.countdown_hours))
        CountdownSeparator()
        CountdownUnit(value = minutes, label = stringResource(Res.string.countdown_minutes))
        CountdownSeparator()
        CountdownUnit(value = seconds, label = stringResource(Res.string.countdown_seconds))
    }
}

@Composable
private fun CountdownUnit(value: Long, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(Dimens.paddingSmall),
        ) {
            Text(
                text = value.toString().padStart(2, '0'),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = Dimens.paddingSmall,
                    vertical = Dimens.paddingExtraSmall,
                ),
            )
        }
        Spacer(modifier = Modifier.height(Dimens.paddingExtraSmall))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun CountdownSeparator() {
    Text(
        text = stringResource(Res.string.countdown_separator),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold,
    )
}

// ---------------------------------------------------------------------------
// Preview — uses a fixed future timestamp so the preview always renders
// ---------------------------------------------------------------------------

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun CountdownCardPreview() {
    // 48 hours from a fixed reference — safe to show in preview
    val fixedTargetMs = PREVIEW_BASE_TIMESTAMP_MS + (PREVIEW_HOURS * MILLIS_PER_HOUR)
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.paddingMedium),
        ) {
            CountdownCard(
                targetEpochMs = fixedTargetMs,
                title = "EVENT STARTS IN",
                dateLabel = "Saturday 27 April 2026",
            )
        }
    }
}
