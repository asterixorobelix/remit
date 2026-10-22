package com.asterixorobelix.remit.domain.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeRecentTasks(limit: Int): Flow<List<Task>>
    suspend fun search(query: String, limit: Int): List<Task>
    val syncStatus: StateFlow<SyncStatus>
    suspend fun create(title: String)
    suspend fun toggle(task: Task)
    suspend fun delete(task: Task)

    /**
     * Sends queued changes and merges the server snapshot. With [force] the server is contacted
     * even when nothing is queued (pull to refresh) and operations in backoff are retried now.
     */
    suspend fun sync(force: Boolean = false): Result<Unit>
}
