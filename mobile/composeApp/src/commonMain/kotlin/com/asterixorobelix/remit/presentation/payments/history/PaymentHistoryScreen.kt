package com.asterixorobelix.remit.presentation.payments.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_payment_row
import com.asterixorobelix.remit.generated.resources.history_amount
import com.asterixorobelix.remit.generated.resources.history_empty_description
import com.asterixorobelix.remit.generated.resources.history_empty_title
import com.asterixorobelix.remit.generated.resources.history_error_message
import com.asterixorobelix.remit.generated.resources.history_error_title
import com.asterixorobelix.remit.generated.resources.history_title
import com.asterixorobelix.remit.presentation.payments.PaymentTestTags
import com.asterixorobelix.remit.presentation.payments.formatted
import com.asterixorobelix.remit.presentation.payments.formattedLocal
import com.asterixorobelix.remit.presentation.payments.label
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.EmailAddress
import com.asterixorobelix.remit.shared.domain.model.Money
import com.asterixorobelix.remit.shared.domain.model.Payment
import com.asterixorobelix.remit.shared.domain.model.PaymentStatus
import com.asterixorobelix.remit.ui.components.BadgeStatus
import com.asterixorobelix.remit.ui.components.EmptyState
import com.asterixorobelix.remit.ui.components.FullScreenError
import com.asterixorobelix.remit.ui.components.FullScreenLoading
import com.asterixorobelix.remit.ui.components.StatusBadge
import com.asterixorobelix.remit.ui.preview.ScreenStates
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

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
        modifier = modifier.fillMaxSize().padding(contentPadding),
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

            is PaymentHistoryUiState.Content -> LazyColumn(modifier = inner.testTag(PaymentTestTags.HISTORY_LIST)) {
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
                    text = stringResource(Res.string.history_amount, amount, payment.amount.currency.code),
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

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun PaymentHistoryContentPreview() {
    val states = listOf(
        PaymentHistoryUiState.Loading,
        PaymentHistoryUiState.Empty,
        PaymentHistoryUiState.Error,
        PaymentHistoryUiState.Content(previewPayments),
    )
    ScreenStates(states) { state -> PaymentHistoryContent(state = state, onEvent = {}) }
}

private val previewPayments = listOf(
    previewPayment("pay_4", "bob@example.com", minorUnits = 1050, Currency.USD, PaymentStatus.COMPLETED),
    previewPayment("pay_3", "dave@example.com", minorUnits = 750, Currency.EUR, PaymentStatus.PENDING),
    previewPayment("pay_2", "carol@example.co.uk", minorUnits = 1_000_000, Currency.EUR, PaymentStatus.FAILED),
    previewPayment("pay_1", "erin@example.com", minorUnits = 4000, Currency.USD, PaymentStatus.UNKNOWN),
)

private fun previewPayment(id: String, recipient: String, minorUnits: Long, currency: Currency, status: PaymentStatus) =
    Payment(
        id = id,
        recipient = checkNotNull(EmailAddress.parse(recipient)),
        amount = Money(minorUnits, currency),
        status = status,
        createdAt = Instant.fromEpochMilliseconds(epochMilliseconds = PREVIEW_SENT_AT_MILLIS),
    )

private const val PREVIEW_SENT_AT_MILLIS = 1_792_657_800_000
