package com.example.myproject.util

import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching] but never swallows coroutine cancellation, so structured
 * concurrency keeps working inside suspend functions.
 */
@Suppress("TooGenericExceptionCaught")
inline fun <T> runSuspendCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (error: Throwable) {
    Result.failure(error)
}
