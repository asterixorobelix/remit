package com.example.myproject.domain

import java.time.Instant

data class User(
    val id: Long,
    val email: String,
    val passwordHash: String,
    val displayName: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)
