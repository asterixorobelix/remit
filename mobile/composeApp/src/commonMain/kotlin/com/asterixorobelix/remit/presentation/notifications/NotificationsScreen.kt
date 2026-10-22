package com.asterixorobelix.remit.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.domain.notifications.AppNotification
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.button_retry
import com.asterixorobelix.remit.generated.resources.notifications_empty
import com.asterixorobelix.remit.generated.resources.notifications_empty_description
import com.asterixorobelix.remit.generated.resources.notifications_title
import com.asterixorobelix.remit.ui.components.EmptyState
import com.asterixorobelix.remit.ui.components.ShimmerBox
import com.asterixorobelix.remit.ui.theme.Dimens
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val SKELETON_TITLE_WIDTH = 0.5f
private const val SKELETON_BODY_WIDTH = 0.9f
private const val SKELETON_TIME_WIDTH = 0.3f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(Res.string.notifications_title))
                },
            )
        },
    ) { paddingValues ->
        NotificationsContent(
            uiState = uiState,
            onNotificationClick = viewModel::markAsRead,
            onRefresh = viewModel::refresh,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationsContent(
    uiState: NotificationsUiState,
    onNotificationClick: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is NotificationsUiState.Loading -> {
            NotificationSkeletonList(modifier = modifier)
        }

        is NotificationsUiState.Empty -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(Res.string.notifications_empty),
                    description = stringResource(Res.string.notifications_empty_description),
                )
            }
        }

        is NotificationsUiState.Error -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = stringResource(Res.string.notifications_empty),
                    description = stringResource(uiState.message),
                    actionLabel = stringResource(Res.string.button_retry),
                    onAction = onRefresh,
                )
            }
        }

        is NotificationsUiState.Content -> {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.notifications, key = { it.id }) { notification ->
                        NotificationListItem(
                            notification = notification,
                            onClick = { onNotificationClick(notification.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationListItem(
    notification: AppNotification,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!notification.isRead) {
                    Box(
                        modifier = Modifier
                            .size(Dimens.spacingSmall)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                    )
                    Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                }
                Text(
                    text = notification.title,
                    style = if (notification.isRead) {
                        MaterialTheme.typography.bodyLarge
                    } else {
                        MaterialTheme.typography.titleSmall
                    },
                )
            }
        },
        supportingContent = {
            Column {
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatReceivedAt(notification.receivedAtEpochMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = Dimens.paddingExtraSmall),
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private const val SKELETON_COUNT = 6

@OptIn(ExperimentalTime::class)
private fun formatReceivedAt(epochMs: Long): String {
    val local = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.date} ${local.hour.toString().padStart(2, '0')}:${local.minute.toString().padStart(2, '0')}"
}

@Composable
private fun NotificationSkeletonList(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        repeat(SKELETON_COUNT) {
            NotificationSkeletonItem()
            HorizontalDivider()
        }
    }
}

@Composable
private fun NotificationSkeletonItem() {
    Column(modifier = Modifier.padding(Dimens.paddingMedium)) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(SKELETON_TITLE_WIDTH)
                .height(Dimens.spacingMedium),
        )
        Spacer(modifier = Modifier.height(Dimens.spacingSmall))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(SKELETON_BODY_WIDTH)
                .height(Dimens.spacingCompact),
        )
        Spacer(modifier = Modifier.height(Dimens.spacingExtraSmall))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(SKELETON_TIME_WIDTH)
                .height(Dimens.spacingCompact),
        )
    }
}
