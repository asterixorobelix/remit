package com.asterixorobelix.remit.dto

import kotlinx.serialization.Serializable

/** The single error shape returned by every endpoint. */
@Serializable
data class ErrorResponse(
    val error: String,
)
