package com.example.myproject.data.task

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.myproject.data.db.AppDatabase
import com.example.myproject.data.db.SyncOperation
import com.example.myproject.data.db.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.CoroutineContext

/** SQLDelight access for Tasks. All functions are blocking; callers switch to an IO dispatcher. */
internal class TaskLocalDataSource(database: AppDatabase) {
    private val queries = database.tasksQueries

    fun observeVisible(context: CoroutineContext): Flow<List<TaskEntity>> =
        queries.observeVisible().asFlow().mapToList(context)

    fun observeRecent(limit: Int, context: CoroutineContext): Flow<List<TaskEntity>> =
        queries.observeRecent(limit.toLong()).asFlow().mapToList(context)

    fun search(query: String, limit: Int): List<TaskEntity> =
        queries.searchVisible("%${query.escapeLike()}%", limit.toLong()).executeAsList()

    fun byIds(ids: Collection<String>): List<TaskEntity> =
        if (ids.isEmpty()) emptyList() else queries.selectByIds(ids).executeAsList()

    fun upsert(task: TaskDto) = queries.transaction { upsertRow(task) }

    fun enqueue(operationId: String, taskId: String) = queries.enqueue(operationId, taskId)

    fun pending(now: Long): List<SyncOperation> = queries.pending(now).executeAsList()

    fun pendingCount(): Int = queries.countOperations().executeAsOne().toInt()

    fun markFailed(id: String, attempts: Long, nextAttemptAt: Long, error: String) =
        queries.markFailed(attempts, nextAttemptAt, error, id)

    /**
     * Applies a successful sync atomically:
     * - removes only the queue entries that were sent,
     * - takes the server copy of every task that has no newer local change still queued,
     * - drops local rows the server does not know about unless they still have queued changes.
     */
    fun commitSync(sentOperationIds: List<String>, serverTasks: List<TaskDto>) = queries.transaction {
        sentOperationIds.forEach(queries::deleteOperation)
        serverTasks.filterNot { hasQueuedChange(it.id) }.forEach(::upsertRow)
        val serverIds = serverTasks.mapTo(HashSet()) { it.id }
        queries.selectAll().executeAsList()
            .filter { it.id !in serverIds && !hasQueuedChange(it.id) }
            .forEach { queries.deleteTask(it.id) }
    }

    fun clearAll() = queries.transaction {
        queries.clearQueue()
        queries.deleteAllTasks()
    }

    private fun hasQueuedChange(taskId: String): Boolean =
        queries.countOperationsForTask(taskId).executeAsOne() > 0

    private fun upsertRow(task: TaskDto) {
        queries.updateTask(
            title = task.title,
            completed = task.completed.toLong(),
            deleted = task.deleted.toLong(),
            updatedAt = task.updatedAt,
            createdAt = task.createdAt,
            id = task.id,
        )
        queries.insertTaskIfAbsent(
            task.id,
            task.title,
            task.completed.toLong(),
            task.deleted.toLong(),
            task.updatedAt,
            task.createdAt,
        )
    }
}

private fun Boolean.toLong(): Long = if (this) 1L else 0L

private fun String.escapeLike(): String =
    replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
