package com.asterixorobelix.remit.domain.common

/** Typed failure categories surfaced by repositories. The UI maps each to a string resource. */
enum class DataError {
    NETWORK,
    INVALID_INPUT,
    UNAUTHORIZED,
    RATE_LIMITED,
    SERVER,
    UNKNOWN,
}

class DataException(val error: DataError, cause: Throwable? = null) : Exception(error.name, cause)

/** The [DataError] carried by this failure, or [DataError.UNKNOWN] for unexpected exceptions. */
fun Throwable.toDataError(): DataError = (this as? DataException)?.error ?: DataError.UNKNOWN
