package com.asterixorobelix.remit.data.notifications

import com.asterixorobelix.remit.domain.notifications.AppNotification
import com.asterixorobelix.remit.domain.notifications.NotificationsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Default inbox with no source connected: always empty, never fabricated. */
internal class EmptyNotificationsRepository : NotificationsRepository {
    override fun observeNotifications(): Flow<List<AppNotification>> = flowOf(emptyList())
    override suspend fun refresh(): Result<Unit> = Result.success(Unit)
    override suspend fun markAsRead(id: String) = Unit
}
