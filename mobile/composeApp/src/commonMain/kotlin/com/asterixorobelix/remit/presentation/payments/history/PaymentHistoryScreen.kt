package com.asterixorobelix.remit.presentation.payments.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_payment_row
import com.asterixorobelix.remit.generated.resources.history_empty_description
import com.asterixorobelix.remit.generated.resources.history_empty_title
import com.asterixorobelix.remit.generated.resources.history_error_message
import com.asterixorobelix.remit.generated.resources.history_error_title
import com.asterixorobelix.remit.generated.resources.history_title
import com.asterixorobelix.remit.presentation.payments.PaymentTestTags
import com.asterixorobelix.remit.presentation.payments.formatted
import com.asterixorobelix.remit.presentation.payments.formattedLocal
import com.asterixorobelix.remit.presentation.payments.label
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import com.asterixorobelix.remit.ui.components.BadgeStatus
import com.asterixorobelix.remit.ui.components.EmptyState
import com.asterixorobelix.remit.ui.components.FullScreenError
import com.asterixorobelix.remit.ui.components.FullScreenLoading
import com.asterixorobelix.remit.ui.components.StatusBadge
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: PaymentHistoryViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.history_title)) }) },
    ) { innerPadding ->
        PaymentHistoryContent(
            state = uiState,
            onEvent = viewModel::onEvent,
            contentPadding = innerPadding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentHistoryContent(
    state: PaymentHistoryUiState,
    onEvent: (PaymentHistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    PullToRefreshBox(
        isRefreshing = (state as? PaymentHistoryUiState.Content)?.isRefreshing == true,
        onRefresh = { onEvent(PaymentHistoryEvent.Refresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        val inner = Modifier.fillMaxSize()
        when (state) {
            PaymentHistoryUiState.Loading -> FullScreenLoading(modifier = inner)

            PaymentHistoryUiState.Empty -> Column(
                modifier = inner.testTag(PaymentTestTags.HISTORY_EMPTY),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    title = stringResource(Res.string.history_empty_title),
                    description = stringResource(Res.string.history_empty_description),
                )
            }

            PaymentHistoryUiState.Error -> FullScreenError(
                modifier = inner,
                title = stringResource(Res.string.history_error_title),
                message = stringResource(Res.string.history_error_message),
                onRetry = { onEvent(PaymentHistoryEvent.Refresh) },
            )

            is PaymentHistoryUiState.Content -> LazyColumn(
                modifier = inner.testTag(PaymentTestTags.HISTORY_LIST),
                contentPadding = contentPadding,
            ) {
                items(state.payments, key = Payment::id) { payment ->
                    PaymentRow(payment)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: Payment, modifier: Modifier = Modifier) {
    val amount = payment.amount.formatted()
    val time = payment.createdAt.formattedLocal()
    val status = stringResource(payment.status.label)
    val description = stringResource(Res.string.cd_payment_row, amount, payment.recipient.value, time, status)
    ListItem(
        modifier = modifier
            .testTag(PaymentTestTags.HISTORY_ITEM)
            .clearAndSetSemantics { contentDescription = description },
        leadingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        headlineContent = { Text(payment.recipient.value) },
        supportingContent = { Text(time) },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingExtraSmall),
            ) {
                Text(
                    text = "$amount ${payment.amount.currency.code}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusBadge(status = payment.status.badge, label = status)
            }
        },
    )
}

private val PaymentStatus.badge: BadgeStatus
    get() = when (this) {
        PaymentStatus.COMPLETED -> BadgeStatus.Success
        PaymentStatus.FAILED -> BadgeStatus.Error
        PaymentStatus.PENDING, PaymentStatus.UNKNOWN -> BadgeStatus.Warning
    }
