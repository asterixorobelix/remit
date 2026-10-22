package com.asterixorobelix.remit.configuration

/** Reads a configuration value by name; [SystemEnv] in production, a map in tests. */
typealias EnvLookup = (String) -> String?

val SystemEnv: EnvLookup = System::getenv

/**
 * Application configuration loaded from environment variables.
 *
 * Nothing loads `.env` files automatically: export variables in the shell or set them on the
 * deployment platform. See `backend/.env.example` for every supported variable.
 *
 * Defaults fail closed: an unset `APP_ENVIRONMENT` means production, which disables Swagger UI
 * and allows no CORS origins unless they are configured explicitly.
 */
data class AppConfig(
    val environment: Environment,
    val server: ServerConfig = ServerConfig(),
    val cors: CorsConfig = CorsConfig(),
    val features: FeatureFlags = FeatureFlags(),
    val remoteConfig: RemoteConfig = RemoteConfig(),
) {
    companion object {
        /** Loads configuration from [env]; the environment decides the defaults. */
        fun load(env: EnvLookup = SystemEnv): AppConfig {
            val environment = Environment.current(env)
            return AppConfig(
                environment = environment,
                server = ServerConfig.fromEnv(env),
                cors = CorsConfig.fromEnv(env, environment),
                features = FeatureFlags.fromEnv(env, environment),
                remoteConfig = RemoteConfig.fromEnv(env),
            )
        }
    }
}

/**
 * Server configuration.
 *
 * [trustedProxyHops] is the number of reverse proxies in front of the server that append to
 * `X-Forwarded-For`. The client address is taken from that position so it cannot be spoofed.
 * Use 0 when the server is exposed directly.
 */
data class ServerConfig(
    val port: Int = DEFAULT_PORT,
    val host: String = DEFAULT_HOST,
    val shutdownGracePeriodMs: Long = DEFAULT_SHUTDOWN_PERIOD,
    val shutdownTimeoutMs: Long = DEFAULT_SHUTDOWN_TIMEOUT,
    val trustedProxyHops: Int = DEFAULT_TRUSTED_PROXY_HOPS,
) {
    companion object {
        const val DEFAULT_PORT = 8085
        const val DEFAULT_HOST = "0.0.0.0"
        const val DEFAULT_SHUTDOWN_PERIOD = 1000L
        const val DEFAULT_SHUTDOWN_TIMEOUT = 5000L
        const val DEFAULT_TRUSTED_PROXY_HOPS = 1

        fun fromEnv(env: EnvLookup) = ServerConfig(
            port = env.int("PORT", DEFAULT_PORT),
            host = env.string("HOST", DEFAULT_HOST),
            shutdownGracePeriodMs = env.long("SHUTDOWN_GRACE_PERIOD_MS", DEFAULT_SHUTDOWN_PERIOD),
            shutdownTimeoutMs = env.long("SHUTDOWN_TIMEOUT_MS", DEFAULT_SHUTDOWN_TIMEOUT),
            trustedProxyHops = env.int("TRUSTED_PROXY_HOPS", DEFAULT_TRUSTED_PROXY_HOPS).coerceAtLeast(0),
        )
    }
}

/**
 * CORS configuration for browser clients. Mobile apps do not need CORS.
 *
 * Entries are `host[:port]` (http and https allowed) or `scheme://host[:port]`.
 */
data class CorsConfig(
    val allowedHosts: List<String> = emptyList(),
    val allowCredentials: Boolean = false,
) {
    companion object {
        val DEVELOPMENT_HOSTS = listOf(
            "localhost:3000",
            "localhost:5173",
            "127.0.0.1:3000",
            "127.0.0.1:5173",
        )

        fun fromEnv(env: EnvLookup, environment: Environment): CorsConfig {
            val allowedHosts = env("CORS_ALLOWED_HOSTS")
                ?.split(",")
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?: if (environment.isDevelopment) DEVELOPMENT_HOSTS else emptyList()
            return CorsConfig(
                allowedHosts = allowedHosts,
                allowCredentials = env.boolean("CORS_ALLOW_CREDENTIALS", false),
            )
        }
    }
}

/**
 * Feature flags for conditional functionality.
 */
data class FeatureFlags(
    val swaggerEnabled: Boolean = true,
) {
    companion object {
        fun fromEnv(env: EnvLookup, environment: Environment) = FeatureFlags(
            swaggerEnabled = env.boolean("SWAGGER_ENABLED", !environment.isProduction),
        )
    }
}

/**
 * Values served by `GET /api/v1/config` to mobile clients.
 */
data class RemoteConfig(
    val minVersion: String = DEFAULT_MIN_VERSION,
    val latestVersion: String = BuildInfo.version,
    val forceUpdate: Boolean = false,
    val maintenanceMode: Boolean = false,
) {
    companion object {
        const val DEFAULT_MIN_VERSION = "0.0.0"

        fun fromEnv(env: EnvLookup) = RemoteConfig(
            minVersion = env.string("MIN_APP_VERSION", DEFAULT_MIN_VERSION),
            latestVersion = env.string("LATEST_APP_VERSION", BuildInfo.version),
            forceUpdate = env.boolean("FORCE_UPDATE", false),
            maintenanceMode = env.boolean("MAINTENANCE_MODE", false),
        )
    }
}

private fun EnvLookup.string(name: String, default: String): String =
    this(name)?.takeIf(String::isNotBlank) ?: default

private fun EnvLookup.int(name: String, default: Int): Int = this(name)?.trim()?.toIntOrNull() ?: default

private fun EnvLookup.long(name: String, default: Long): Long = this(name)?.trim()?.toLongOrNull() ?: default

private fun EnvLookup.boolean(name: String, default: Boolean): Boolean =
    this(name)?.trim()?.lowercase()?.toBooleanStrictOrNull() ?: default
