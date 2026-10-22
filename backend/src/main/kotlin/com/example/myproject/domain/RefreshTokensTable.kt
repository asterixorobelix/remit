package com.example.myproject.domain

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp

private const val TOKEN_ID_MAX_LENGTH = 36

/** Refresh token registry keyed by the JWT id (`jti`); see V3 migration. */
object RefreshTokensTable : Table("refresh_tokens") {
    val id = varchar("id", TOKEN_ID_MAX_LENGTH)
    val userId = long("user_id")
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val revokedAt = timestamp("revoked_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
