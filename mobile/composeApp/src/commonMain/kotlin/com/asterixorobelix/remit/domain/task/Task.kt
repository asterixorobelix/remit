package com.asterixorobelix.remit.domain.task

import com.asterixorobelix.remit.domain.common.DataError

/** Backend limit for task titles (`POST /api/v1/tasks/sync` rejects longer titles). */
const val TASK_TITLE_MAX_LENGTH = 240

data class Task(
    val id: String,
    val title: String,
    val completed: Boolean,
    val updatedAt: Long,
    val createdAt: Long,
)

sealed interface SyncStatus {
    /** Everything local has reached the server (or sync is disabled for this app). */
    data object Idle : SyncStatus
    data object Syncing : SyncStatus

    /** [count] local changes are queued and will be sent on the next sync. */
    data class Pending(val count: Int) : SyncStatus

    /** The last sync failed; queued changes are kept and retried with backoff. */
    data class Error(val error: DataError, val pendingCount: Int) : SyncStatus
}

/** Whether Tasks talk to the backend. Local-only apps (no `backend`/`auth`) never enqueue sync work. */
data class TaskSyncSettings(val syncEnabled: Boolean)
