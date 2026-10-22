package com.asterixorobelix.remit.domain

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.javatime.timestamp
import java.time.Instant

const val EMAIL_MAX_LENGTH = 255
const val DISPLAY_NAME_MAX_LENGTH = 100
private const val PASSWORD_HASH_MAX_LENGTH = 255

object UsersTable : LongIdTable("users") {
    val email = varchar("email", EMAIL_MAX_LENGTH).uniqueIndex()
    val passwordHash = varchar("password_hash", PASSWORD_HASH_MAX_LENGTH)
    val displayName = varchar("display_name", DISPLAY_NAME_MAX_LENGTH)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}
