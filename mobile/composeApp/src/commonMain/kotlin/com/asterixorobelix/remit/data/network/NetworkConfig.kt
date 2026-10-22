package com.asterixorobelix.remit.data.network

/**
 * Configuration for the API HttpClient with timeout and retry settings.
 *
 * This consolidates all network configuration in one place for easy customization
 * and dependency injection.
 *
 * @property connectTimeoutMs Connection timeout in milliseconds (default: 10 seconds)
 * @property requestTimeoutMs Request timeout in milliseconds (default: 30 seconds)
 * @property socketTimeoutMs Socket timeout in milliseconds (default: 30 seconds)
 * @property retryPolicy Retry policy for failed requests (default: RetryPolicy.DEFAULT)
 */
data class NetworkConfig(
    val connectTimeoutMs: Long = DEFAULT_CONNECT_TIMEOUT_MS,
    val requestTimeoutMs: Long = DEFAULT_REQUEST_TIMEOUT_MS,
    val socketTimeoutMs: Long = DEFAULT_SOCKET_TIMEOUT_MS,
    val retryPolicy: RetryPolicy = RetryPolicy.DEFAULT,
) {
    companion object {
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10_000L
        private const val DEFAULT_REQUEST_TIMEOUT_MS = 30_000L
        private const val DEFAULT_SOCKET_TIMEOUT_MS = 30_000L

        /**
         * Default configuration with standard timeout values and default retry policy.
         */
        val DEFAULT = NetworkConfig()

        /**
         * Configuration optimized for slow networks with longer timeouts.
         */
        val SLOW_NETWORK = NetworkConfig(
            connectTimeoutMs = 30_000L,
            requestTimeoutMs = 60_000L,
            socketTimeoutMs = 60_000L,
            retryPolicy = RetryPolicy.AGGRESSIVE,
        )

        /**
         * Configuration for quick operations with shorter timeouts and no retry.
         */
        val FAST_FAIL = NetworkConfig(
            connectTimeoutMs = 5_000L,
            requestTimeoutMs = 10_000L,
            socketTimeoutMs = 10_000L,
            retryPolicy = RetryPolicy.NO_RETRY,
        )
    }
}
