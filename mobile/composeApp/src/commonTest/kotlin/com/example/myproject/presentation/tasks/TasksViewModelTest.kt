package com.example.myproject.presentation.tasks

import app.cash.turbine.test
import com.example.myproject.domain.common.DataError
import com.example.myproject.domain.task.CreateTaskUseCase
import com.example.myproject.domain.task.DeleteTaskUseCase
import com.example.myproject.domain.task.ObserveSyncStatusUseCase
import com.example.myproject.domain.task.ObserveTasksUseCase
import com.example.myproject.domain.task.SyncStatus
import com.example.myproject.domain.task.SyncTasksUseCase
import com.example.myproject.domain.task.TASK_TITLE_MAX_LENGTH
import com.example.myproject.domain.task.Task
import com.example.myproject.domain.task.ToggleTaskUseCase
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.message_error_network
import com.example.myproject.testing.FakeTaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class TasksViewModelTest {
    private val repository = FakeTaskRepository()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = TasksViewModel(
        observeTasks = ObserveTasksUseCase(repository),
        observeSyncStatus = ObserveSyncStatusUseCase(repository),
        createTask = CreateTaskUseCase(repository),
        toggleTask = ToggleTaskUseCase(repository),
        deleteTask = DeleteTaskUseCase(repository),
        syncTasks = SyncTasksUseCase(repository),
    )

    @Test
    fun startsEmptyAndPullsFromServer() {
        val viewModel = viewModel()
        assertIs<TasksUiState.Empty>(viewModel.state.value)
        assertEquals(listOf(true), repository.syncCalls)
    }

    @Test
    fun addCreatesTaskClearsDraftAndSyncsQueuedChange() {
        val viewModel = viewModel()
        viewModel.onEvent(TasksEvent.DraftChanged("  Write tests "))
        viewModel.onEvent(TasksEvent.Add)

        val content = assertIs<TasksUiState.Content>(viewModel.state.value)
        assertEquals("Write tests", content.tasks.single().title)
        assertEquals("", content.draft)
        assertEquals(listOf(true, false), repository.syncCalls)
    }

    @Test
    fun blankDraftDoesNotCreateOrSync() {
        val viewModel = viewModel()
        viewModel.onEvent(TasksEvent.DraftChanged("   "))
        viewModel.onEvent(TasksEvent.Add)
        assertEquals(emptyList(), repository.created)
        assertEquals(listOf(true), repository.syncCalls)
    }

    @Test
    fun draftIsCappedAtBackendTitleLimit() {
        val viewModel = viewModel()
        viewModel.onEvent(TasksEvent.DraftChanged("x".repeat(TASK_TITLE_MAX_LENGTH + 1)))
        assertEquals(TASK_TITLE_MAX_LENGTH, assertIs<TasksUiState.Empty>(viewModel.state.value).draft.length)
    }

    @Test
    fun toggleAndDeleteDelegateToUseCases() {
        val task = Task("1", "A", completed = false, updatedAt = 1, createdAt = 1)
        repository.tasks.value = listOf(task)
        val viewModel = viewModel()
        viewModel.onEvent(TasksEvent.Toggle(task))
        viewModel.onEvent(TasksEvent.Delete(task))
        assertEquals(listOf(task), repository.toggled)
        assertEquals(listOf(task), repository.deleted)
    }

    @Test
    fun syncStatusIsExposedInState() {
        val viewModel = viewModel()
        repository.status.value = SyncStatus.Pending(2)
        assertEquals(SyncStatus.Pending(2), assertIs<TasksUiState.Empty>(viewModel.state.value).syncStatus)
    }

    @Test
    fun userRefreshFailureShowsMessageAndStopsRefreshing() = runTest {
        val viewModel = viewModel()
        repository.syncFailure = DataError.NETWORK
        viewModel.effects.test {
            viewModel.onEvent(TasksEvent.Refresh)
            assertEquals(TasksEffect.ShowMessage(Res.string.message_error_network), awaitItem())
        }
        assertFalse(assertIs<TasksUiState.Empty>(viewModel.state.value).isRefreshing)
    }

    @Test
    fun backgroundSyncFailureDoesNotInterruptWithSnackbar() = runTest {
        repository.syncFailure = DataError.NETWORK
        val viewModel = viewModel()
        viewModel.effects.test {
            viewModel.onEvent(TasksEvent.DraftChanged("Offline task"))
            viewModel.onEvent(TasksEvent.Add)
            expectNoEvents()
        }
    }

    @Test
    fun databaseFailureShowsErrorAndRefreshRetries() {
        repository.observeFailure = IllegalStateException("disk")
        val viewModel = viewModel()
        assertIs<TasksUiState.Error>(viewModel.state.value)

        repository.observeFailure = null
        viewModel.onEvent(TasksEvent.Refresh)
        assertIs<TasksUiState.Empty>(viewModel.state.value)
    }
}
