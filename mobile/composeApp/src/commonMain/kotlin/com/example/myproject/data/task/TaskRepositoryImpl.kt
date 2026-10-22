package com.example.myproject.data.task

import com.example.myproject.data.db.SyncOperation
import com.example.myproject.data.db.TaskEntity
import com.example.myproject.data.network.toDataException
import com.example.myproject.domain.session.UserDataCleaner
import com.example.myproject.domain.task.SyncStatus
import com.example.myproject.domain.task.Task
import com.example.myproject.domain.task.TaskRepository
import com.example.myproject.domain.task.TaskSyncSettings
import com.example.myproject.platform.AppDispatchers
import com.example.myproject.util.currentTimeMillis
import com.example.myproject.util.generateId
import com.example.myproject.util.runSuspendCatching
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val MAX_RETRY_DELAY_MS = 60L * 60L * 1000L
private const val INITIAL_RETRY_DELAY_MS = 1_000L
private const val MAX_RETRY_EXPONENT = 12

/** Backend limit for `POST /api/v1/tasks/sync`. */
internal const val MAX_SYNC_BATCH_SIZE = 500

/**
 * Local-first Tasks repository.
 *
 * Concurrency contract: [writeMutex] serializes every database write (user edits, sync commit,
 * sign-out cleanup). The network call runs outside that lock, so edits made while a sync is in
 * flight are never lost: the commit only removes the queue entries it actually sent and keeps
 * any row that still has a queued change. [syncMutex] allows one sync at a time, and
 * [userGeneration] discards a sync that finishes after the user signed out.
 */
internal class TaskRepositoryImpl(
    private val local: TaskLocalDataSource,
    private val remote: TaskRemoteDataSource,
    private val settings: TaskSyncSettings,
    private val dispatchers: AppDispatchers,
) : TaskRepository, UserDataCleaner {
    private val writeMutex = Mutex()
    private val syncMutex = Mutex()
    private val userGeneration = MutableStateFlow(0)
    private val mutableSyncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    override val syncStatus: StateFlow<SyncStatus> = mutableSyncStatus.asStateFlow()

    override fun observeTasks(): Flow<List<Task>> =
        local.observeVisible(dispatchers.io).map { rows -> rows.map(TaskEntity::toDomain) }

    override fun observeRecentTasks(limit: Int): Flow<List<Task>> =
        local.observeRecent(limit, dispatchers.io).map { rows -> rows.map(TaskEntity::toDomain) }

    override suspend fun search(query: String, limit: Int): List<Task> = withContext(dispatchers.io) {
        local.search(query, limit).map(TaskEntity::toDomain)
    }

    override suspend fun create(title: String) {
        if (title.isBlank()) return
        val now = currentTimeMillis()
        persist(TaskDto(generateId(), title, completed = false, deleted = false, updatedAt = now, createdAt = now))
    }

    override suspend fun toggle(task: Task) = persist(task.toDto(completed = !task.completed))

    override suspend fun delete(task: Task) = persist(task.toDto(deleted = true))

    private suspend fun persist(task: TaskDto) = withContext(dispatchers.io) {
        writeMutex.withLock {
            local.upsert(task)
            if (settings.syncEnabled) local.enqueue(generateId(), task.id)
        }
        publishQueueState()
    }

    override suspend fun sync(force: Boolean): Result<Unit> {
        if (!settings.syncEnabled) return Result.success(Unit)
        return syncMutex.withLock { withContext(dispatchers.io) { syncLocked(force) } }
    }

    /**
     * Sends only rows with queued changes, in batches of at most [MAX_SYNC_BATCH_SIZE] tasks (the
     * backend rejects larger batches), committing after each successful batch. A forced sync with
     * nothing queued still sends one empty batch to pull the server snapshot.
     */
    private suspend fun syncLocked(force: Boolean): Result<Unit> {
        val now = currentTimeMillis()
        val generation = userGeneration.value
        val due = writeMutex.withLock { local.pending(if (force) Long.MAX_VALUE else now) }
        if (due.isEmpty() && !force) {
            publishQueueState()
            return Result.success(Unit)
        }

        mutableSyncStatus.value = SyncStatus.Syncing
        val batches = due.groupBy { it.task_id }.entries.chunked(MAX_SYNC_BATCH_SIZE).ifEmpty { listOf(emptyList()) }
        batches.forEachIndexed { index, batch ->
            val taskIds = batch.map { it.key }
            val rows = writeMutex.withLock { local.byIds(taskIds) }
            val response = runSuspendCatching { remote.sync(rows.map(TaskEntity::toDto)) }.getOrElse { error ->
                val unsent = batches.drop(index).flatMap { remaining -> remaining.flatMap { it.value } }
                return recordFailure(unsent, error, now)
            }
            writeMutex.withLock {
                if (userGeneration.value == generation) {
                    local.commitSync(batch.flatMap { entry -> entry.value.map { it.id } }, response.tasks)
                }
            }
        }
        publishQueueState()
        return Result.success(Unit)
    }

    private suspend fun recordFailure(
        operations: List<SyncOperation>,
        error: Throwable,
        now: Long,
    ): Result<Unit> {
        val failure = error.toDataException()
        val pendingCount = writeMutex.withLock {
            operations.forEach { operation ->
                val attempts = operation.attempt_count + 1
                local.markFailed(operation.id, attempts, now + backoffDelay(attempts), failure.error.name)
            }
            local.pendingCount()
        }
        mutableSyncStatus.value = SyncStatus.Error(failure.error, pendingCount)
        return Result.failure(failure)
    }

    override suspend fun clearUserData() = withContext(dispatchers.io) {
        writeMutex.withLock {
            userGeneration.value += 1
            local.clearAll()
        }
        mutableSyncStatus.value = SyncStatus.Idle
    }

    private suspend fun publishQueueState() {
        val count = writeMutex.withLock { local.pendingCount() }
        mutableSyncStatus.value = if (count == 0) SyncStatus.Idle else SyncStatus.Pending(count)
    }
}

internal fun backoffDelay(attempts: Long): Long {
    val exponent = attempts.coerceIn(0L, MAX_RETRY_EXPONENT.toLong()).toInt()
    return (INITIAL_RETRY_DELAY_MS shl exponent).coerceAtMost(MAX_RETRY_DELAY_MS)
}

private fun TaskEntity.toDomain() = Task(id, title, completed != 0L, updated_at, created_at)

private fun TaskEntity.toDto() = TaskDto(id, title, completed != 0L, deleted != 0L, updated_at, created_at)

private fun Task.toDto(completed: Boolean = this.completed, deleted: Boolean = false) =
    TaskDto(id, title, completed, deleted, updatedAt = currentTimeMillis(), createdAt = createdAt)
