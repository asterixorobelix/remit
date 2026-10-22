package com.example.myproject.domain.task

class ObserveTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke() = repository.observeTasks()
}

class ObserveRecentTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(limit: Int = DEFAULT_LIMIT) = repository.observeRecentTasks(limit)

    private companion object {
        const val DEFAULT_LIMIT = 3
    }
}

class ObserveSyncStatusUseCase(private val repository: TaskRepository) {
    operator fun invoke() = repository.syncStatus
}

class CreateTaskUseCase(private val repository: TaskRepository) {
    /**
     * Returns false (and stores nothing) for blank titles. Titles are capped at
     * [TASK_TITLE_MAX_LENGTH] because the backend rejects the whole sync batch otherwise.
     */
    suspend operator fun invoke(title: String): Boolean {
        val trimmed = title.trim().take(TASK_TITLE_MAX_LENGTH).trim()
        if (trimmed.isEmpty()) return false
        repository.create(trimmed)
        return true
    }
}

class ToggleTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) = repository.toggle(task)
}

class DeleteTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(task: Task) = repository.delete(task)
}

class SyncTasksUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(force: Boolean = false) = repository.sync(force)
}
