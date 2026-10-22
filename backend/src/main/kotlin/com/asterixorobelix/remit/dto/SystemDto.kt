package com.asterixorobelix.remit.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServiceInfoResponse(
    val service: String,
    val version: String,
)

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: Long,
    val version: String,
)

@Serializable
data class ReadinessResponse(
    val status: String,
)

@Serializable
data class StatusResponse(
    val api: String,
    val version: String,
)
