package com.example.myproject.service

import com.example.myproject.domain.TASK_TITLE_MAX_LENGTH
import com.example.myproject.domain.TasksTable
import com.example.myproject.domain.UsersTable
import com.example.myproject.dto.TaskDto
import com.example.myproject.plugins.dbQuery
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.insertIgnore
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Offline-first task reconciliation. Tasks are owned by a user, soft-deleted via `deleted`, and
 * resolved by last-write-wins on `updatedAt`.
 */
class TaskService(
    private val database: Database,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun all(userId: Long): List<TaskDto> = database.dbQuery { tasksOf(userId) }

    /**
     * Applies [incoming] snapshots and returns the user's full task list, or null when the user no
     * longer exists. Client timestamps are clamped to at most [MAX_CLOCK_SKEW] in the future.
     * @throws ValidationException when the batch or any task is invalid; nothing is written.
     */
    suspend fun sync(userId: Long, incoming: List<TaskDto>): List<TaskDto>? {
        if (incoming.size > MAX_SYNC_BATCH_SIZE) {
            validationError("A sync batch may contain at most $MAX_SYNC_BATCH_SIZE tasks")
        }
        val latestAllowed = clock.instant().plus(MAX_CLOCK_SKEW).truncatedTo(ChronoUnit.MILLIS)
        val tasks = incoming.mapIndexed { index, task -> task.validated(index, latestAllowed) }

        return database.dbQuery {
            val userExists = !UsersTable.selectAll().where { UsersTable.id eq userId }.empty()
            if (userExists) {
                tasks.forEach { task -> apply(userId, task) }
                tasksOf(userId)
            } else {
                null
            }
        }
    }

    /**
     * Inserts the task, or overwrites the stored row only if the incoming snapshot is newer. Both
     * statements are atomic, so concurrent syncs can neither fail on the same new id nor replace a
     * newer row with an older one.
     */
    private fun apply(userId: Long, task: ValidTask) {
        val inserted = TasksTable.insertIgnore {
            it[TasksTable.userId] = userId
            it[TasksTable.id] = task.id
            it[TasksTable.title] = task.title
            it[TasksTable.completed] = task.completed
            it[TasksTable.deleted] = task.deleted
            it[TasksTable.updatedAt] = task.updatedAt
            it[TasksTable.createdAt] = task.createdAt
        }.insertedCount
        if (inserted > 0) return

        TasksTable.update({
            (TasksTable.userId eq userId) and
                (TasksTable.id eq task.id) and
                (TasksTable.updatedAt less task.updatedAt)
        }) {
            it[TasksTable.title] = task.title
            it[TasksTable.completed] = task.completed
            it[TasksTable.deleted] = task.deleted
            it[TasksTable.updatedAt] = task.updatedAt
        }
    }

    private fun tasksOf(userId: Long): List<TaskDto> =
        TasksTable.selectAll()
            .where { TasksTable.userId eq userId }
            .orderBy(TasksTable.createdAt to SortOrder.ASC, TasksTable.id to SortOrder.ASC)
            .map { it.toTask() }

    companion object {
        const val MAX_SYNC_BATCH_SIZE = 500
        val MAX_CLOCK_SKEW: Duration = Duration.ofMinutes(5)
    }
}

private data class ValidTask(
    val id: String,
    val title: String,
    val completed: Boolean,
    val deleted: Boolean,
    val updatedAt: Instant,
    val createdAt: Instant,
)

private val TASK_ID_PATTERN = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

private fun TaskDto.validated(index: Int, latestAllowed: Instant): ValidTask {
    val trimmedTitle = title.trim()
    val problem = when {
        !TASK_ID_PATTERN.matches(id) -> "id must be a UUID"
        trimmedTitle.isEmpty() -> "title must not be blank"
        trimmedTitle.length > TASK_TITLE_MAX_LENGTH -> "title must be at most $TASK_TITLE_MAX_LENGTH characters"
        updatedAt < 0 || createdAt < 0 -> "timestamps must be non-negative epoch milliseconds"
        else -> null
    }
    if (problem != null) validationError("Task at index $index: $problem")

    return ValidTask(
        id = id,
        title = trimmedTitle,
        completed = completed,
        deleted = deleted,
        updatedAt = minOf(Instant.ofEpochMilli(updatedAt), latestAllowed),
        createdAt = minOf(Instant.ofEpochMilli(createdAt), latestAllowed),
    )
}

private fun ResultRow.toTask() = TaskDto(
    id = this[TasksTable.id],
    title = this[TasksTable.title],
    completed = this[TasksTable.completed],
    deleted = this[TasksTable.deleted],
    updatedAt = this[TasksTable.updatedAt].toEpochMilli(),
    createdAt = this[TasksTable.createdAt].toEpochMilli(),
)
