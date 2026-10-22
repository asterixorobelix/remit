package com.example.myproject.domain

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

const val TASK_ID_MAX_LENGTH = 36
const val TASK_TITLE_MAX_LENGTH = 240

/** Task ids are client-generated UUIDs, unique per user (see V4 migration). */
object TasksTable : Table("tasks") {
    val userId = long("user_id")
    val id = varchar("id", TASK_ID_MAX_LENGTH)
    val title = varchar("title", TASK_TITLE_MAX_LENGTH)
    val completed = bool("completed")
    val deleted = bool("deleted")
    val updatedAt = timestamp("updated_at")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(userId, id)
}
