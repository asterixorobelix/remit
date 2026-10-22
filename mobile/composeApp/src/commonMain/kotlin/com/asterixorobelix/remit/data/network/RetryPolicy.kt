package com.asterixorobelix.remit.data.network

import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Configuration for retry behavior with exponential backoff and jitter.
 *
 * The delay calculation uses exponential backoff with optional jitter to prevent
 * thundering herd problems when multiple clients retry simultaneously.
 *
 * @property maxRetries Maximum number of retry attempts (default: 3)
 * @property initialDelayMs Initial delay in milliseconds before first retry (default: 1000)
 * @property maxDelayMs Maximum delay cap in milliseconds (default: 10000)
 * @property multiplier Exponential backoff multiplier (default: 2.0)
 * @property jitterFactor Randomness factor to spread out retries (default: 0.1)
 */
data class RetryPolicy(
    val maxRetries: Int = DEFAULT_MAX_RETRIES,
    val initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    val maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    val multiplier: Double = DEFAULT_MULTIPLIER,
    val jitterFactor: Double = DEFAULT_JITTER_FACTOR,
) {
    companion object {
        private const val DEFAULT_MAX_RETRIES = 3
        private const val DEFAULT_INITIAL_DELAY_MS = 1000L
        private const val DEFAULT_MAX_DELAY_MS = 10000L
        private const val DEFAULT_MULTIPLIER = 2.0
        private const val DEFAULT_JITTER_FACTOR = 0.1

        /**
         * Default retry policy with standard settings.
         */
        val DEFAULT = RetryPolicy()

        /**
         * Aggressive retry policy for critical operations.
         */
        val AGGRESSIVE = RetryPolicy(
            maxRetries = 5,
            initialDelayMs = 500L,
            maxDelayMs = 30000L,
        )

        /**
         * No retry policy - fails immediately on first error.
         */
        val NO_RETRY = RetryPolicy(maxRetries = 0)
    }
}

/**
 * Calculates the delay for a given retry attempt using exponential backoff with jitter.
 *
 * Formula:
 * 1. Base delay = initialDelayMs * (multiplier ^ attempt)
 * 2. Clamped delay = min(baseDelay, maxDelayMs)
 * 3. Jittered delay = clampedDelay * (1 + random(-jitterFactor, +jitterFactor))
 *
 * @param attempt Zero-based retry attempt number (0 = first retry, 1 = second retry, etc.)
 * @param random Random instance for jitter calculation (injectable for testing)
 * @return Delay in milliseconds before the next retry attempt
 */
fun RetryPolicy.calculateDelay(attempt: Int, random: Random = Random): Long {
    if (attempt < 0) return initialDelayMs

    // Calculate base exponential delay
    val baseDelay = initialDelayMs * multiplier.pow(attempt.toDouble())

    // Clamp to maximum delay
    val clampedDelay = min(baseDelay, maxDelayMs.toDouble())

    // Apply jitter: delay * (1 + random(-jitterFactor, +jitterFactor))
    val jitterRange = clampedDelay * jitterFactor
    val jitter = random.nextDouble(-jitterRange, jitterRange)

    return (clampedDelay + jitter).toLong().coerceAtLeast(0L)
}
