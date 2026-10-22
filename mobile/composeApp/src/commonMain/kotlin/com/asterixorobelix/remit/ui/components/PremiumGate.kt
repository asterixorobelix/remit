package com.asterixorobelix.remit.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.asterixorobelix.remit.domain.purchase.PremiumRepository
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.paywall_buy_button
import com.asterixorobelix.remit.generated.resources.premium_gate_message
import com.asterixorobelix.remit.ui.theme.AppTheme
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Conditionally shows [content] or a locked placeholder based on premium status.
 *
 * The locked state shows a lock icon, [lockedTitle], and an upgrade CTA button.
 * When the user taps the CTA, [onUpgrade] is called so the parent can navigate
 * to the paywall.
 *
 * Two usage patterns:
 *
 * **Pattern A — hide content behind a lock (subscription wall):**
 * ```kotlin
 * PremiumGate(
 *     lockedTitle = "Advanced Analytics",
 *     lockedMessage = "Upgrade to see detailed insights.",
 *     onUpgrade = { navController.navigate(PaywallRoute(trigger = "analytics")) },
 * ) {
 *     AdvancedAnalyticsContent()
 * }
 * ```
 *
 * **Pattern B — check before an action (no visible lock, just navigate away):**
 * Use [CheckPremiumAccessUseCase] in your ViewModel instead of this composable.
 *
 * @param lockedTitle Short headline shown when the user is not premium
 * @param lockedMessage Optional supporting text shown below the title
 * @param onUpgrade Called when the user taps the upgrade CTA button
 * @param modifier Optional modifier
 * @param upgradeLabel Label for the upgrade CTA button
 * @param content Content shown when the user has premium access
 */
@Composable
fun PremiumGate(
    lockedTitle: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
    lockedMessage: String = stringResource(Res.string.premium_gate_message),
    upgradeLabel: String = stringResource(Res.string.paywall_buy_button),
    premiumRepository: PremiumRepository = koinInject(),
    content: @Composable () -> Unit,
) {
    val isPremium by premiumRepository.isPremium.collectAsState()

    if (isPremium) {
        content()
    } else {
        LockedContent(
            title = lockedTitle,
            message = lockedMessage,
            upgradeLabel = upgradeLabel,
            onUpgrade = onUpgrade,
            modifier = modifier,
        )
    }
}

@Composable
private fun LockedContent(
    title: String,
    message: String,
    upgradeLabel: String,
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .padding(Dimens.paddingLarge)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(Dimens.iconSizeLarge),
            )

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Dimens.spacingExtraSmall))

            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

            PrimaryButton(
                onClick = onUpgrade,
                text = upgradeLabel,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
@Suppress("UnusedPrivateMember")
private fun PremiumGateLockedPreview() {
    AppTheme {
        LockedContent(
            title = "Advanced Analytics",
            message = "Upgrade to see detailed insights and reports.",
            upgradeLabel = "Unlock Premium",
            onUpgrade = {},
            modifier = Modifier.padding(Dimens.paddingMedium),
        )
    }
}
