package com.example.myproject.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String,
    val title: String,
    val completed: Boolean,
    val deleted: Boolean,
    val updatedAt: Long,
    val createdAt: Long,
)

@Serializable data class TaskSyncRequest(val tasks: List<TaskDto>)

@Serializable data class TaskSyncResponse(val tasks: List<TaskDto>, val syncedAt: Long)
