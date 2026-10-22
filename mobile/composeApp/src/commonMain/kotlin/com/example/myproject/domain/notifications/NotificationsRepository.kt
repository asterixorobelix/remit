package com.example.myproject.domain.notifications

import kotlinx.coroutines.flow.Flow

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val receivedAtEpochMs: Long,
    val isRead: Boolean,
)

/**
 * In-app notification inbox. The template ships `EmptyNotificationsRepository`, so the screen shows
 * its empty state until you connect a real source (push payloads, a backend inbox endpoint, or
 * local reminders).
 */
interface NotificationsRepository {
    fun observeNotifications(): Flow<List<AppNotification>>
    suspend fun refresh(): Result<Unit>
    suspend fun markAsRead(id: String)
}

class ObserveNotificationsUseCase(private val repository: NotificationsRepository) {
    operator fun invoke() = repository.observeNotifications()
}

class RefreshNotificationsUseCase(private val repository: NotificationsRepository) {
    suspend operator fun invoke() = repository.refresh()
}

class MarkNotificationReadUseCase(private val repository: NotificationsRepository) {
    suspend operator fun invoke(id: String) = repository.markAsRead(id)
}
