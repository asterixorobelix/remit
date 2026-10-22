package com.asterixorobelix.remit.presentation.payments.send

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.currency_eur
import com.asterixorobelix.remit.generated.resources.currency_usd
import com.asterixorobelix.remit.generated.resources.send_amount_label
import com.asterixorobelix.remit.generated.resources.send_currency_label
import com.asterixorobelix.remit.generated.resources.send_description
import com.asterixorobelix.remit.generated.resources.send_recipient_label
import com.asterixorobelix.remit.generated.resources.send_retry
import com.asterixorobelix.remit.generated.resources.send_submit
import com.asterixorobelix.remit.generated.resources.send_success
import com.asterixorobelix.remit.generated.resources.send_title
import com.asterixorobelix.remit.generated.resources.send_view_history
import com.asterixorobelix.remit.generated.resources.unfinished_body
import com.asterixorobelix.remit.generated.resources.unfinished_discard
import com.asterixorobelix.remit.generated.resources.unfinished_title
import com.asterixorobelix.remit.presentation.payments.PaymentTestTags
import com.asterixorobelix.remit.presentation.payments.formatted
import com.asterixorobelix.remit.presentation.payments.groupThousands
import com.asterixorobelix.remit.presentation.payments.message
import com.asterixorobelix.remit.shared.domain.model.Currency
import com.asterixorobelix.remit.shared.domain.model.DecimalAmount
import com.asterixorobelix.remit.shared.domain.model.DecimalParseResult
import com.asterixorobelix.remit.shared.domain.validation.PaymentField
import com.asterixorobelix.remit.shared.domain.validation.PaymentLimits
import com.asterixorobelix.remit.shared.domain.validation.ValidationError
import com.asterixorobelix.remit.ui.components.AppTextButton
import com.asterixorobelix.remit.ui.components.PrimaryButton
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

private const val CURRENCY_WEIGHT = 0.7f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendPaymentScreen(
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SendPaymentViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currentOnViewHistory by rememberUpdatedState(onViewHistory)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is SendPaymentEffect.PaymentSent -> {
                    val payment = effect.payment
                    val result = snackbarHostState.showSnackbar(
                        message = getString(
                            Res.string.send_success,
                            payment.amount.formatted(),
                            payment.recipient.value,
                        ),
                        actionLabel = getString(Res.string.send_view_history),
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) currentOnViewHistory()
                }

                is SendPaymentEffect.PaymentFailed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = getString(effect.reason.message),
                        actionLabel = if (effect.reason.retryable) getString(Res.string.send_retry) else null,
                        duration = SnackbarDuration.Long,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.onEvent(SendPaymentEvent.Submit)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.send_title)) }) },
    ) { innerPadding ->
        when (val state = uiState) {
            is SendPaymentUiState.Content -> SendPaymentContent(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
fun SendPaymentContent(
    state: SendPaymentUiState.Content,
    onEvent: (SendPaymentEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontalPadding),
    ) {
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        Text(
            text = stringResource(Res.string.send_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.unfinished) {
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            UnfinishedPaymentCard(state = state, onDiscard = { onEvent(SendPaymentEvent.DiscardUnfinished) })
        }
        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
        OutlinedTextField(
            value = state.recipientEmail,
            onValueChange = { onEvent(SendPaymentEvent.RecipientChanged(it)) },
            modifier = Modifier.fillMaxWidth().testTag(PaymentTestTags.RECIPIENT),
            enabled = !state.isSubmitting,
            label = { Text(stringResource(Res.string.send_recipient_label)) },
            isError = state.errors[PaymentField.RECIPIENT_EMAIL] != null,
            supportingText = state.errors[PaymentField.RECIPIENT_EMAIL]?.let { error -> { FieldError(error) } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            shape = MaterialTheme.shapes.medium,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingCompact)) {
            OutlinedTextField(
                value = state.amount,
                onValueChange = { onEvent(SendPaymentEvent.AmountChanged(it)) },
                modifier = Modifier.weight(1f).testTag(PaymentTestTags.AMOUNT),
                enabled = !state.isSubmitting,
                label = { Text(stringResource(Res.string.send_amount_label)) },
                prefix = { Text(state.currency.symbol) },
                isError = state.errors[PaymentField.AMOUNT] != null,
                supportingText = state.errors[PaymentField.AMOUNT]?.let { error -> { FieldError(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onEvent(SendPaymentEvent.Submit)
                }),
                shape = MaterialTheme.shapes.medium,
            )
            CurrencyPicker(
                selected = state.currency,
                enabled = !state.isSubmitting,
                onSelect = { onEvent(SendPaymentEvent.CurrencySelected(it)) },
                modifier = Modifier.weight(CURRENCY_WEIGHT),
            )
        }
        Spacer(modifier = Modifier.height(Dimens.spacingLarge))
        PrimaryButton(
            onClick = {
                focusManager.clearFocus()
                onEvent(SendPaymentEvent.Submit)
            },
            text = stringResource(Res.string.send_submit),
            modifier = Modifier.fillMaxWidth().testTag(PaymentTestTags.SUBMIT),
            enabled = !state.isSubmitting,
            loading = state.isSubmitting,
            icon = Icons.AutoMirrored.Filled.Send,
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSection))
    }
}

@Composable
private fun UnfinishedPaymentCard(
    state: SendPaymentUiState.Content,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val amount = (DecimalAmount.parse(state.amount) as? DecimalParseResult.Parsed)
        ?.amount?.toMoney(state.currency)?.formatted() ?: state.amount
    Card(
        modifier = modifier.fillMaxWidth().testTag(PaymentTestTags.UNFINISHED),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(modifier = Modifier.padding(Dimens.paddingMedium)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = stringResource(Res.string.unfinished_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            Text(
                text = stringResource(Res.string.unfinished_body, amount, state.recipientEmail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            AppTextButton(
                onClick = onDiscard,
                text = stringResource(Res.string.unfinished_discard),
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun FieldError(error: ValidationError) {
    val text = if (error == ValidationError.AMOUNT_ABOVE_LIMIT) {
        stringResource(error.message, groupThousands(PaymentLimits.MAX_WHOLE_UNITS.toString()))
    } else {
        stringResource(error.message)
    }
    Text(text)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyPicker(
    selected: Currency,
    enabled: Boolean,
    onSelect: (Currency) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.code,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(stringResource(Res.string.send_currency_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                .testTag(PaymentTestTags.CURRENCY),
            shape = MaterialTheme.shapes.medium,
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Currency.entries.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(stringResource(currency.label)) },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                    },
                    modifier = Modifier.testTag(PaymentTestTags.CURRENCY_OPTION_PREFIX + currency.code),
                )
            }
        }
    }
}

private val Currency.label: StringResource
    get() = when (this) {
        Currency.USD -> Res.string.currency_usd
        Currency.EUR -> Res.string.currency_eur
    }
