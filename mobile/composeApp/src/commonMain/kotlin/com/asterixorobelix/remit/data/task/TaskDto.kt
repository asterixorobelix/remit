package com.asterixorobelix.remit.data.task

import kotlinx.serialization.Serializable

@Serializable internal data class TaskDto(
    val id: String,
    val title: String,
    val completed: Boolean,
    val deleted: Boolean,
    val updatedAt: Long,
    val createdAt: Long,
)

@Serializable internal data class TaskSyncRequest(val tasks: List<TaskDto>)

@Serializable internal data class TaskSyncResponse(val tasks: List<TaskDto>, val syncedAt: Long)
