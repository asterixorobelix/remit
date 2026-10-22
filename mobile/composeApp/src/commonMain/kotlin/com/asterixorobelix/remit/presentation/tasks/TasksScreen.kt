package com.asterixorobelix.remit.presentation.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.asterixorobelix.remit.domain.task.SyncStatus
import com.asterixorobelix.remit.domain.task.Task
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.cd_add_task
import com.asterixorobelix.remit.generated.resources.cd_delete_task
import com.asterixorobelix.remit.generated.resources.cd_refresh
import com.asterixorobelix.remit.generated.resources.message_empty_tasks
import com.asterixorobelix.remit.generated.resources.screen_tasks_title
import com.asterixorobelix.remit.generated.resources.task_title_hint
import com.asterixorobelix.remit.generated.resources.tasks_sync_failed
import com.asterixorobelix.remit.generated.resources.tasks_sync_pending
import com.asterixorobelix.remit.generated.resources.tasks_sync_syncing
import com.asterixorobelix.remit.presentation.common.toMessage
import com.asterixorobelix.remit.ui.components.FullScreenError
import com.asterixorobelix.remit.ui.theme.Dimens
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TasksScreen(
    onMessage: suspend (String) -> Unit,
    viewModel: TasksViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is TasksEffect.ShowMessage -> currentOnMessage(getString(effect.message))
            }
        }
    }
    TasksContent(state, viewModel::onEvent)
}

/** Stateless Tasks UI; internal so the desktop screenshot tests can render it with fake state. */
@Composable
internal fun TasksContent(state: TasksUiState, onEvent: (TasksEvent) -> Unit) {
    when (state) {
        TasksUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        is TasksUiState.Error -> FullScreenError(
            message = stringResource(state.message),
            onRetry = { onEvent(TasksEvent.Refresh) },
        )

        is TasksUiState.Empty -> TasksList(emptyList(), state.draft, state.isRefreshing, state.syncStatus, onEvent)

        is TasksUiState.Content -> TasksList(state.tasks, state.draft, state.isRefreshing, state.syncStatus, onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksList(
    tasks: List<Task>,
    draft: String,
    refreshing: Boolean,
    syncStatus: SyncStatus,
    onEvent: (TasksEvent) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Dimens.screenHorizontalPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.screen_tasks_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onEvent(TasksEvent.Refresh) }, enabled = !refreshing) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.cd_refresh))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { onEvent(TasksEvent.DraftChanged(it)) },
                label = { Text(stringResource(Res.string.task_title_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            FilledIconButton(onClick = { onEvent(TasksEvent.Add) }, enabled = draft.isNotBlank()) {
                Icon(Icons.Default.Add, contentDescription = stringResource(Res.string.cd_add_task))
            }
        }
        SyncStatusLine(syncStatus)
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { onEvent(TasksEvent.Refresh) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
            ) {
                if (tasks.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.message_empty_tasks),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingLarge),
                        )
                    }
                }
                items(tasks, key = Task::id) { task -> TaskRow(task, onEvent) }
            }
        }
    }
}

@Composable
private fun TaskRow(task: Task, onEvent: (TasksEvent) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = task.completed, onCheckedChange = { onEvent(TasksEvent.Toggle(task)) })
        Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        IconButton(onClick = { onEvent(TasksEvent.Delete(task)) }) {
            Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.cd_delete_task))
        }
    }
}

@Composable
private fun SyncStatusLine(status: SyncStatus) {
    val text = when (status) {
        SyncStatus.Idle -> return
        SyncStatus.Syncing -> stringResource(Res.string.tasks_sync_syncing)
        is SyncStatus.Pending -> pluralStringResource(Res.plurals.tasks_sync_pending, status.count, status.count)
        is SyncStatus.Error -> stringResource(Res.string.tasks_sync_failed, stringResource(status.error.toMessage()))
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = if (status is SyncStatus.Error) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    )
}
