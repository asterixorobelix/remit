package com.asterixorobelix.remit.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * The single error shape returned by every endpoint. [code] is a stable, machine-readable reason
 * (for example `VALIDATION_FAILED`); [details] lists validation codes. Both are omitted when unset.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ErrorResponse(
    val error: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val code: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val details: List<String> = emptyList(),
)
