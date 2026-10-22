package com.asterixorobelix.remit.presentation.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.asterixorobelix.remit.domain.common.toDataError
import com.asterixorobelix.remit.domain.task.CreateTaskUseCase
import com.asterixorobelix.remit.domain.task.DeleteTaskUseCase
import com.asterixorobelix.remit.domain.task.ObserveSyncStatusUseCase
import com.asterixorobelix.remit.domain.task.ObserveTasksUseCase
import com.asterixorobelix.remit.domain.task.SyncStatus
import com.asterixorobelix.remit.domain.task.SyncTasksUseCase
import com.asterixorobelix.remit.domain.task.TASK_TITLE_MAX_LENGTH
import com.asterixorobelix.remit.domain.task.Task
import com.asterixorobelix.remit.domain.task.ToggleTaskUseCase
import com.asterixorobelix.remit.generated.resources.Res
import com.asterixorobelix.remit.generated.resources.tasks_error_load
import com.asterixorobelix.remit.presentation.common.toMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

sealed interface TasksUiState {
    data object Loading : TasksUiState

    data class Content(
        val tasks: List<Task>,
        val draft: String = "",
        val isRefreshing: Boolean = false,
        val syncStatus: SyncStatus = SyncStatus.Idle,
    ) : TasksUiState

    data class Empty(
        val draft: String = "",
        val isRefreshing: Boolean = false,
        val syncStatus: SyncStatus = SyncStatus.Idle,
    ) : TasksUiState

    data class Error(val message: StringResource) : TasksUiState
}

sealed interface TasksEvent {
    data class DraftChanged(val value: String) : TasksEvent
    data object Add : TasksEvent
    data class Toggle(val task: Task) : TasksEvent
    data class Delete(val task: Task) : TasksEvent
    data object Refresh : TasksEvent
}

sealed interface TasksEffect {
    data class ShowMessage(val message: StringResource) : TasksEffect
}

class TasksViewModel(
    private val observeTasks: ObserveTasksUseCase,
    private val observeSyncStatus: ObserveSyncStatusUseCase,
    private val createTask: CreateTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val syncTasks: SyncTasksUseCase,
) : ViewModel() {
    private val draft = MutableStateFlow("")
    private val refreshing = MutableStateFlow(false)
    private val mutableState = MutableStateFlow<TasksUiState>(TasksUiState.Loading)
    val state: StateFlow<TasksUiState> = mutableState.asStateFlow()
    private val effectChannel = Channel<TasksEffect>(Channel.BUFFERED)
    val effects: Flow<TasksEffect> = effectChannel.receiveAsFlow()

    private val reloadRequests = MutableStateFlow(0)

    init {
        viewModelScope.launch { observeState().collect { mutableState.value = it } }
        sync(force = true, userInitiated = false)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeState(): Flow<TasksUiState> = reloadRequests.flatMapLatest {
        combine(observeTasks(), observeSyncStatus(), draft, refreshing) { tasks, status, text, isRefreshing ->
            if (tasks.isEmpty()) {
                TasksUiState.Empty(text, isRefreshing, status)
            } else {
                TasksUiState.Content(tasks, text, isRefreshing, status)
            }
        }.catch { emit(TasksUiState.Error(Res.string.tasks_error_load)) }
    }

    fun onEvent(event: TasksEvent) {
        when (event) {
            is TasksEvent.DraftChanged -> draft.value = event.value.take(TASK_TITLE_MAX_LENGTH)

            TasksEvent.Add -> viewModelScope.launch {
                if (createTask(draft.value)) {
                    draft.value = ""
                    sync(force = false, userInitiated = false)
                }
            }

            is TasksEvent.Toggle -> viewModelScope.launch {
                toggleTask(event.task)
                sync(force = false, userInitiated = false)
            }

            is TasksEvent.Delete -> viewModelScope.launch {
                deleteTask(event.task)
                sync(force = false, userInitiated = false)
            }

            TasksEvent.Refresh -> {
                if (mutableState.value is TasksUiState.Error) reloadRequests.value += 1
                sync(force = true, userInitiated = true)
            }
        }
    }

    /**
     * Background syncs report failures through the persistent sync status; only a user-initiated
     * refresh also shows a snackbar.
     */
    private fun sync(force: Boolean, userInitiated: Boolean) {
        viewModelScope.launch {
            if (userInitiated) refreshing.value = true
            syncTasks(force).onFailure { error ->
                if (userInitiated) effectChannel.send(TasksEffect.ShowMessage(error.toDataError().toMessage()))
            }
            if (userInitiated) refreshing.value = false
        }
    }
}
