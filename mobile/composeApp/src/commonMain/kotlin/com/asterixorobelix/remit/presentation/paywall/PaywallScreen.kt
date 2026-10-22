package com.asterixorobelix.remit.presentation.paywall

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.paywall_buy_button
import com.asterixorobelix.remit.generated.resources.paywall_cd_close
import com.asterixorobelix.remit.generated.resources.paywall_cd_purchase
import com.asterixorobelix.remit.generated.resources.paywall_cd_try_again
import com.asterixorobelix.remit.generated.resources.paywall_feature_1
import com.asterixorobelix.remit.generated.resources.paywall_feature_2
import com.asterixorobelix.remit.generated.resources.paywall_feature_3
import com.asterixorobelix.remit.generated.resources.paywall_pending
import com.asterixorobelix.remit.generated.resources.paywall_pro_unlocked
import com.asterixorobelix.remit.generated.resources.paywall_restore
import com.asterixorobelix.remit.generated.resources.paywall_subtitle
import com.asterixorobelix.remit.generated.resources.paywall_title
import com.asterixorobelix.remit.generated.resources.paywall_try_again
import com.asterixorobelix.remit.ui.components.AppTextButton
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.theme.Dimens
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val POST_PURCHASE_DELAY_MS = 1000L

/**
 * Full-screen paywall. Copy comes from string resources (`paywall_*`); edit those for your
 * product. [onPurchaseComplete] receives the trigger that opened the paywall.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onDismiss: () -> Unit,
    onPurchaseComplete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(Res.string.paywall_title)
    val subtitle = stringResource(Res.string.paywall_subtitle)
    val features = listOf(
        stringResource(Res.string.paywall_feature_1),
        stringResource(Res.string.paywall_feature_2),
        stringResource(Res.string.paywall_feature_3),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val proUnlockedMessage = stringResource(Res.string.paywall_pro_unlocked)
    val currentOnPurchaseComplete by rememberUpdatedState(onPurchaseComplete)

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is PaywallEffect.PurchaseComplete -> {
                    snackbarHostState.showSnackbar(
                        message = proUnlockedMessage,
                        duration = SnackbarDuration.Short,
                    )
                    delay(POST_PURCHASE_DELAY_MS)
                    currentOnPurchaseComplete(effect.trigger)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { PaywallTopBar(onDismiss = onDismiss) },
    ) { contentPadding ->
        PaywallContent(
            uiState = uiState,
            title = title,
            subtitle = subtitle,
            features = features,
            onPurchase = { viewModel.purchase() },
            onRetry = { viewModel.retry() },
            onRestore = { viewModel.restorePurchases() },
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = Dimens.screenHorizontalPadding),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaywallTopBar(onDismiss: () -> Unit) {
    TopAppBar(
        title = { },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(Res.string.paywall_cd_close),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@Suppress("LongParameterList")
@Composable
private fun PaywallContent(
    uiState: PaywallUiState,
    title: String,
    subtitle: String,
    features: List<String>,
    onPurchase: () -> Unit,
    onRetry: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingSmall))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

        features.forEach { feature ->
            FeatureChecklistItem(text = feature)
            Spacer(modifier = Modifier.height(Dimens.spacingCompact))
        }

        Spacer(modifier = Modifier.height(Dimens.spacingExtraLarge))

        if (uiState is PaywallUiState.Error) {
            Text(
                text = stringResource(uiState.message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = Dimens.spacingSmall),
            )
        }
        if (uiState is PaywallUiState.Pending) {
            Text(
                text = stringResource(Res.string.paywall_pending),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Dimens.spacingSmall),
            )
        }

        val isLoading = uiState is PaywallUiState.Purchasing || uiState is PaywallUiState.Restoring
        val isError = uiState is PaywallUiState.Error
        val isReady = uiState is PaywallUiState.Ready

        PrimaryButton(
            onClick = { if (isError) onRetry() else onPurchase() },
            text = if (isError) {
                stringResource(Res.string.paywall_try_again)
            } else {
                stringResource(Res.string.paywall_buy_button)
            },
            enabled = isReady || isError,
            loading = isLoading,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = if (isError) {
                stringResource(Res.string.paywall_cd_try_again)
            } else {
                stringResource(Res.string.paywall_cd_purchase)
            },
        )

        AppTextButton(
            onClick = onRestore,
            text = stringResource(Res.string.paywall_restore),
            enabled = isReady,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Single feature checklist row with a tertiary-colored checkmark icon.
 */
@Composable
private fun FeatureChecklistItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(Dimens.iconSizeMedium),
        )
        Spacer(modifier = Modifier.width(Dimens.spacingCompact))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
